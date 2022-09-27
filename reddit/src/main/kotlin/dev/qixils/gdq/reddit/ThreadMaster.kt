package dev.qixils.gdq.reddit

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dean.jraw.RedditClient
import net.dean.jraw.http.OkHttpNetworkAdapter
import net.dean.jraw.http.UserAgent
import net.dean.jraw.oauth.Credentials
import net.dean.jraw.oauth.OAuthHelper
import org.slf4j.LoggerFactory
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.kotlin.dataClassFieldDiscoverer
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.io.path.notExists
import kotlin.io.path.writeLines
import kotlin.system.exitProcess

object ThreadMaster {
    private val logger = LoggerFactory.getLogger(ThreadMaster::class.java)
    private val config: Config
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val reddit: RedditClient

    init {
        logger.info("Starting bot...")

        // get config file and create it if it doesn't exist
        val path = Paths.get("vodthread.yml")
        if (path.notExists()) {
            val defaultConfig = ThreadMaster::class.java.getResourceAsStream("/vodthread.yml")!!
            path.writeLines(defaultConfig.bufferedReader().readLines())
            logger.info("Created default config file at ${path.toAbsolutePath()}; please edit it and restart the bot.")
            exitProcess(0)
        }

        // do weird magic from configurate docs to allow configurate to deserialize data classes
        val loader = YamlConfigurationLoader.builder()
            .path(path)
            .defaultOptions {
                it.serializers { s ->
                    s.registerAnnotatedObjects(
                        ObjectMapper.factoryBuilder()
                            .addDiscoverer(dataClassFieldDiscoverer())
                            .build()
                    )
                }
            }
            .build()

        // load config
        val node: CommentedConfigurationNode
        try {
            node = loader.load()
        } catch (e: ConfigurateException) {
            logger.error("Failed to load config", e)
            exitProcess(1)
        }

        try {
            config = node.get(Config::class.java)!!
        } catch (e: Exception) {
            logger.error("Config is invalid", e)
            exitProcess(1)
        }

        // init reddit
        val userAgent = UserAgent("bot", "dev.qixils.gdq", "1.0.0", "noellekiq")
        reddit = OAuthHelper.automatic(OkHttpNetworkAdapter(userAgent), config.credentials.toCredentials())
    }

    @JvmStatic
    fun main(args: Array<String>) {
        // load schedules
        logger.info("Loading threads...")
        val managers = config.threads.map { ThreadManager(reddit, it) }
        scheduler.scheduleAtFixedRate({
            runBlocking { managers.forEach { launch { it.run() } } }
        }, 0, config.waitMinutes, TimeUnit.MINUTES)
    }
}