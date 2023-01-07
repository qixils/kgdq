package dev.qixils.gdq.discord

import club.speedrun.vods.db.Database
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.jdabuilder.intents
import dev.minn.jda.ktx.jdabuilder.light
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.MessageType
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.messages.MessageRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.kotlin.dataClassFieldDiscoverer
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Paths
import kotlin.io.path.notExists
import kotlin.io.path.writeLines
import kotlin.system.exitProcess

object Bot {
    val db = Database("discord")
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val config: Config
    val jda: JDA
    private val schedules: MutableList<ScheduleManager> = mutableListOf()

    init {
        logger.debug("Starting bot...")

        // get config file and create it if it doesn't exist
        val path = Paths.get("vodchat.yml")
        if (path.notExists()) {
            val defaultConfig = Bot::class.java.getResourceAsStream("/vodchat.yml")!!
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

        // init jda
        MessageRequest.setDefaultMentions(emptySet())

        jda = light(config.token, enableCoroutines=true) {
            intents += listOf(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
        }

        // register event listeners
        jda.listener<ReadyEvent> {
            logger.info("Loading schedules...")
            schedules.addAll(config.events.map { event -> ScheduleManager(this@Bot, event) })
        }

        jda.listener<MessageReceivedEvent> {
            if (it.author.id != jda.selfUser.id) return@listener
            if (it.message.type != MessageType.CHANNEL_PINNED_ADD) return@listener
            if (config.events.any { event -> event.channels.contains(it.messageIdLong) }) return@listener
            it.message.delete().queue()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        // no-op
    }
}

