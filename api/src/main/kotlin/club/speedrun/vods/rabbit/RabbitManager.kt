package club.speedrun.vods.rabbit

import club.speedrun.vods.marathon.DatabaseManager
import club.speedrun.vods.marathon.RunOverrides
import club.speedrun.vods.marathon.TwitchVOD
import com.github.twitch4j.helix.TwitchHelix
import com.github.twitch4j.helix.TwitchHelixBuilder
import com.github.twitch4j.helix.domain.StreamList
import com.netflix.hystrix.HystrixCommand
import com.rabbitmq.client.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.litote.kmongo.coroutine.replaceOne
import org.litote.kmongo.eq
import org.litote.kmongo.push
import org.litote.kmongo.setValue
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

    fun declareQueue(queueName: String, twitchChannel: String, db: DatabaseManager) {
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
    private val db: DatabaseManager,
) : DeliverCallback {
    private val logger: Logger = LoggerFactory.getLogger("DeliverHandler:$stream")
    private val loadedAt = Instant.now()
    private val status = runBlocking { db.getOrCreateStatus(queue) }

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
        val runnable = Runnable { runBlocking {
            db.statuses.replaceOne(status)
            logger.info("Updated MongoDB with run ${status.currentRun} and game scene ${status.usingGameScene}")
        } }
        executor.schedule(runnable, minDuration.toSeconds(), TimeUnit.SECONDS)
    }

    private suspend fun handleRunChanged(runChanged: SCActiveRunChanged) {
        if (status.currentRun != runChanged.run?.horaroId) {
            // update instance
            status.currentRun = runChanged.run?.horaroId
            // update db
            if (Duration.between(loadedAt, Instant.now()) > minDuration)
                db.statuses.updateOneById(
                    status._id,
                    setValue(ScheduleStatus::currentRun, status.currentRun)
                )
            // log
            logger.info("Run is now ${status.currentRun}")
        }
    }

    private suspend fun handleSceneChanged(sceneChanged: OBSSceneChanged) = coroutineScope {
        val updateDb = Duration.between(loadedAt, Instant.now()) > minDuration
        val updateRunDb = updateDb
                && status.usingGameScene == false
                && sceneChanged.gameScene
                && status.currentRun != null
        if (updateRunDb) {
            // Updating start time of run
            val runStart = sceneChanged.time.instant
            logger.info("Updating start time of run ${status.currentRun} to ${sceneChanged.time.iso}")
            db.runs.updateOne(
                RunOverrides::horaroId eq status.currentRun,
                setValue(RunOverrides::startTime, runStart)
            )
            // Fetching current stream to add VOD link
            // | URL to grab an auth token:
            // | https://id.twitch.tv/oauth2/authorize?client_id=CLIENT_ID&response_type=code&redirect_uri=https://vods.speedrun.club/api/auth/twitch_callback&scope=
            val streams = twitch.streams(System.getenv("TWITCH_AUTH_TOKEN"), userLogins = listOf(stream)).execute()
            val stream = streams.streams.firstOrNull() ?: return@coroutineScope
            val vod = TwitchVOD(stream.id, Duration.between(stream.startedAtInstant, runStart))
            // Adding VOD link to run
            logger.info("Adding VOD link ${vod.asURL()} to run ${status.currentRun}")
            db.runs.updateOne(
                RunOverrides::horaroId eq status.currentRun,
                push(RunOverrides::twitchVODs, vod)
            )
        }

        if (status.usingGameScene != sceneChanged.gameScene) {
            // update instance
            status.usingGameScene = sceneChanged.gameScene
            // update db
            if (updateDb)
                db.statuses.updateOneById(
                    status._id,
                    setValue(ScheduleStatus::usingGameScene, status.usingGameScene)
                )
        }
        // log
        logger.info("Scene is now ${status.usingGameScene} (${sceneChanged.scene})")
    }

    override fun handle(consumerTag: String, delivery: Delivery) = runBlocking {
        val message = String(delivery.body, StandardCharsets.UTF_8)
        val routingKey = delivery.envelope.routingKey
        logger.info("Received message from $routingKey")
        logger.debug(message)

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
    authToken: String,
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