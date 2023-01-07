package club.speedrun.vods.rabbit

import club.speedrun.vods.marathon.GdqDatabaseManager
import club.speedrun.vods.marathon.TwitchVOD
import com.github.twitch4j.helix.TwitchHelix
import com.github.twitch4j.helix.TwitchHelixBuilder
import com.github.twitch4j.helix.domain.StreamList
import com.github.twitch4j.helix.domain.VideoList
import com.netflix.hystrix.HystrixCommand
import com.rabbitmq.client.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object RabbitManager {
    private val logger: Logger = LoggerFactory.getLogger(RabbitManager::class.java)
    private val connection = run {
        val factory = ConnectionFactory()
        factory.useSslProtocol()
        factory.username = System.getenv("MQ_USERNAME")
        factory.password = System.getenv("MQ_PASSWORD")
        factory.host = System.getenv("MQ_URL")
        factory.port = System.getenv("MQ_PORT")?.toInt() ?: 5672
        factory.virtualHost = System.getenv("MQ_VHOST") ?: "/"
        factory.newConnection()
    } ?: throw IllegalStateException("Failed to connect to RabbitMQ")
    private val channel = connection.createChannel()
    private const val consumerTag = "SpeedrunClubVODs"
    private val cancelCallback = CancelCallback { consumerTag: String? ->
        logger.info("$consumerTag was canceled")
    }
    private var queues: Int = 0

    fun declareQueue(queueName: String, twitchChannel: String, db: GdqDatabaseManager) {
        if (channel == null) {
            logger.error("Failed to connect to RabbitMQ")
            return
        }
        channel.basicConsume(
            queueName,
            false,
            "$consumerTag-${queues++}",
            DeliverHandler(queueName, twitchChannel, channel, db),
            cancelCallback
        )
    }
}

class DeliverHandler(
    private val queue: String,
    private val stream: String,
    private val channel: Channel,
    private val db: GdqDatabaseManager,
) : DeliverCallback {
    private val logger: Logger = LoggerFactory.getLogger("DeliverHandler:$stream")
    private val loadedAt = Instant.now()
    private val status = db.getOrCreateStatus(queue)

    companion object {
        private val executor = Executors.newSingleThreadScheduledExecutor()
        private val json: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
        private val twitch = TwitchHelixBuilder.builder()
            .withClientId(System.getenv("TWITCH_CLIENT_ID"))
            .withClientSecret(System.getenv("TWITCH_CLIENT_SECRET"))
            .build()
        private val minDuration = Duration.ofSeconds(3)
    }

    init {
        val runnable = Runnable {
            db.statuses.update(status)
            logger.info("Updated DB with run ${status.currentRun} and game scene ${status.usingGameScene}")
        }
        executor.schedule(runnable, minDuration.toSeconds(), TimeUnit.SECONDS)
    }

    private fun handleRunChanged(runChanged: SCActiveRunChanged) {
        if (status.currentRun != runChanged.run?.horaroId) {
            // update instance
            status.currentRun = runChanged.run?.horaroId
            // update db
            if (Duration.between(loadedAt, Instant.now()) > minDuration)
                db.statuses.update(status)
            // log
            logger.info("Run is now ${status.currentRun}")
        }
    }

    private suspend fun handleSceneChanged(sceneChanged: OBSSceneChanged) = coroutineScope {
        // sceneChanged.gameScene is not reliable (seemingly always true)
        // so here's some workaround magic to try to get it to work
        val isGameScene: Boolean = if (!sceneChanged.gameScene)
            false
        else if (sceneChanged.scene != "Game Layout")
            return@coroutineScope
        else if (sceneChanged.action == Action.END)
            false
        else if (sceneChanged.action == Action.START)
            true
        else
            return@coroutineScope

        val updateDb = Duration.between(loadedAt, Instant.now()) > minDuration
        val updateRunDb = updateDb
                && status.usingGameScene == false
                && isGameScene
                && status.currentRun != null
        if (updateRunDb) {
            // Start fetching current stream for VOD link timestamp
            val streams = async { twitch.streams(userLogins = listOf(stream)).execute() }
            // Updating start time of run
            val runStart = sceneChanged.time.instant
            logger.info("Updating start time of run ${status.currentRun} to ${sceneChanged.time.iso}")
            val overrides = db.getOrCreateRunOverrides(null, status.currentRun)
            overrides.startTime = runStart
            // Fetching stream's video (should always be the latest video)
            val stream = streams.await().streams.firstOrNull() ?: return@coroutineScope
            val videos = async { twitch.videos(userId = stream.userId, type = "archive", limit = 1).execute() }
            val video = videos.await().videos.firstOrNull() ?: return@coroutineScope
            // Create VOD object
            val vod = TwitchVOD(video.id, Duration.between(stream.startedAtInstant, runStart))
            // Adding VOD link to run
            logger.info("Adding VOD link ${vod.url} to run ${status.currentRun}")
            overrides.twitchVODs.add(vod)
            db.runs.update(overrides)
        }

        if (status.usingGameScene != isGameScene) {
            // update instance
            status.usingGameScene = isGameScene
            // update db
            if (updateDb)
                db.statuses.update(status)
            // log
            logger.info("Scene is now ${status.usingGameScene}")
        }
    }

    override fun handle(consumerTag: String, delivery: Delivery) = runBlocking {
        val message = String(delivery.body, StandardCharsets.UTF_8)
        val routingKey = delivery.envelope.routingKey
        logger.debug("Received message from $routingKey")
        logger.trace(message)

        try {
            when {
                routingKey.contains("run.changed") -> handleRunChanged(json.decodeFromString(SCActiveRunChanged.serializer(), message))
                routingKey.contains("obs.scene") -> handleSceneChanged(json.decodeFromString(OBSSceneChanged.serializer(), message))
                else -> throw IllegalArgumentException("Unknown routing key $routingKey")
            }
            channel.basicAck(delivery.envelope.deliveryTag, false)
        } catch (e: Exception) {
            logger.error("Failed to handle message from $routingKey", e)
            channel.basicNack(delivery.envelope.deliveryTag, false, true)
        }
    }
}

fun TwitchHelix.streams(
    authToken: String? = null,
    after: String? = null,
    before: String? = null,
    limit: Int? = null,
    gameIds: List<String>? = null,
    language: List<String>? = null,
    userIds: List<String>? = null,
    userLogins: List<String>? = null,
): HystrixCommand<StreamList> {
    return getStreams(
        authToken,
        after,
        before,
        limit,
        gameIds,
        language,
        userIds,
        userLogins
    )
}

fun TwitchHelix.videos(
    authToken: String? = null,
    id: String? = null,
    userId: String? = null,
    gameId: String? = null,
    language: String? = null,
    period: String? = null,
    sort: String? = null,
    type: String? = null,
    after: String? = null,
    before: String? = null,
    limit: Int? = null
): HystrixCommand<VideoList> {
    return getVideos(
        authToken,
        id,
        userId,
        gameId,
        language,
        period,
        sort,
        type,
        after,
        before,
        limit
    )
}