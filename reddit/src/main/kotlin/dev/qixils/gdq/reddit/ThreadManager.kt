package dev.qixils.gdq.reddit

import club.speedrun.vods.marathon.EventData
import club.speedrun.vods.marathon.RunData
import club.speedrun.vods.naturalJoinTo
import dev.qixils.gdq.models.Runner

object ThreadManager {

    init {
        // TODO
    }

    fun generateBody(event: EventData, runs: List<RunData>): CharSequence {
        val body = StringBuilder()
        generateHeader(body, event)
        for (run in runs) {
            generateRunRow(body, run)
        }
        return body
    }

    private fun generateHeader(body: StringBuilder, event: EventData) {
        body.append("The event's schedule may change without notice so this thread can be out-of-date by several minutes. ")
            .append("Please check the [event's website](https://gamesdonequick.com/schedule/")
            .append(event.id)
            .append(") for the most accurate reference.\n\n")
            .append("Don't gild the thread, [donate the money instead](https://gamesdonequick.com/tracker/ui/donate/")
            .append(event.short)
            .append(")! \\^_\\^\n\n")
            .append("This thread is powered by data from GamesDoneQuick, Speedrun.com, and the contributors to the [VOD list](https://www.reddit.com/r/VODThread/wiki/")
            .append(event.short)
            .append("vods). Thank you to the volunteers that keep this thread running.\n\n")
            .append("### Links\n\n")
            .append("* [Watch SGDQ 2022](https://twitch.tv/gamesdonequick)\n") // TODO: load event name and twitch name from config
            .append("* [GDQ homepage](https://gamesdonequick.com/)\n")
            .append("* [GDQ YouTube playlist](https://www.youtube.com/c/gamesdonequick)\n") // TODO: fetch playlist ID from config
            .append("* [Automatic thread updater](https://github.com/qixils/kgdq)\n\n")
            .append("Game | Runner / Channel | Time / Link\n")
            .append("--|--|:--:|\n")
    }

    private fun generateRunRow(body: StringBuilder, run: RunData) {
        //// game name, category, SR.com link
        body.append(run.displayName)
        if (run.category.isNotEmpty())
            body.append(" (").append(run.category).append(")")
        // TODO: SRC data
        body.append(" | ")

        //// runners
        naturalJoinTo(body, run.runners) { generateRunner(it) }
        body.append(" | ")

        //// time, VODs
        body.append(generateTime(run))
    }

    private fun generateRunner(runner: Runner): String {
        // Return markdown-formatted name with link if available
        return runner.url()?.let { "[${runner.name}]($it)" } ?: runner.name
    }

    private fun generateTime(run: RunData): CharSequence {
        val time = StringBuilder()
        // add twitch VODs if available
        if (run.twitchVODs.isNotEmpty()) {
            run.twitchVODs.forEachIndexed { index, vod ->
                time.append('[')
                if (index == 0) time.append(run.runTimeText)
                else            time.append(index + 1)
                time.append("](").append(vod.asURL()).append(')')
            }
        } else {
            // otherwise, add run time with no link
            time.append(run.runTimeText)
        }
        // add YouTube VODs if available
        run.youtubeVODs.forEachIndexed { index, vod ->
            time.append(" [YT")
            if (index > 0) time.append(index + 1)
            time.append("](").append(vod.asURL()).append(")")
        }
        // add italics if the run is ongoing
        if (run.isCurrent)
            time.insert(0, '*').append('*')
        return time
    }

    @JvmStatic
    fun main(args: Array<String>) {
        // no-op
    }
}