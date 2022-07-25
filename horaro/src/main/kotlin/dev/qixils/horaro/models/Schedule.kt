package dev.qixils.horaro.models

import dev.qixils.gdq.serializers.DurationAsSecondsSerializer
import dev.qixils.gdq.serializers.InstantAsStringSerializer
import dev.qixils.gdq.serializers.OffsetDateTimeSerializer
import dev.qixils.gdq.serializers.ZoneIdSerializer
import dev.qixils.horaro.Horaro
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * The basic metadata components of a marathon's schedule.
 */
sealed interface BaseSchedule : Identifiable {

    /**
     * The timezone of the schedule.
     */
    val timezone: ZoneId

    /**
     * The start time of the schedule.
     */
    val start: OffsetDateTime

    /**
     * The setup time of the start of the marathon.
     */
    val setup: Duration

    /**
     * The time of the most recent update to the schedule.
     */
    val updated: Instant

    /**
     * The custom data columns of the schedule.
     */
    val columns: List<String>

    /**
     * The custom data columns of the schedule that are hidden from public view.
     *
     * Entries in this list will also appear in the `columns` field;
     * the two are not mutually exclusive.
     *
     * If this schedule is not fetched directly, this field may be empty.
     */
    val hiddenColumns: List<String>?

    /**
     * Fetches an abbreviated version of this schedule.
     *
     * @param hiddenKey optional: the key used to include hidden columns if a "hidden column secret"
     *                            is configured
     * @return this schedule's [Ticker]
     */
    suspend fun ticker(hiddenKey: String? = null): Ticker = Horaro.getTicker(this, hiddenKey)!!

    /**
     * Fetches the full version of this schedule.
     *
     * @param hiddenKey optional: the key used to include hidden columns if a "hidden column secret"
     *                            is configured
     * @return the [FullSchedule] version of this schedule
     */
    suspend fun full(hiddenKey: String? = null): FullSchedule {
        if (this is FullSchedule) return this
        return Horaro.getSchedule(id, hiddenKey)!!
    }
}

/**
 * An implementation of the basic schedule metadata.
 */
@Serializable
data class PartialSchedule(
    override val id: String,
    override val name: String,
    override val slug: String,
    @Serializable(with = ZoneIdSerializer::class) override val timezone: ZoneId,
    @Serializable(with = OffsetDateTimeSerializer::class) override val start: OffsetDateTime,
    @Serializable(with = DurationAsSecondsSerializer::class) @SerialName("setup_t") override val setup: Duration,
    @Serializable(with = InstantAsStringSerializer::class) override val updated: Instant,
    override val link: String,
    @SerialName("hidden_columns") override val hiddenColumns: List<String> = emptyList(),
    override val columns: List<String>,
) : BaseSchedule

/**
 * A full implementation of a marathon's schedule, complete with a list of runs and links.
 */
@Serializable
data class FullSchedule(
    override val id: String,
    override val name: String,
    override val slug: String,
    @Serializable(with = ZoneIdSerializer::class) override val timezone: ZoneId,
    @Serializable(with = OffsetDateTimeSerializer::class) override val start: OffsetDateTime,
    @Serializable(with = DurationAsSecondsSerializer::class) @SerialName("setup_t") override val setup: Duration,
    @Serializable(with = InstantAsStringSerializer::class) override val updated: Instant,
    override val link: String,
    @SerialName("hidden_columns") override val hiddenColumns: List<String> = emptyList(),
    override val columns: List<String>,
    /**
     * The list of runs in the schedule.
     */
    val items: List<Run>,
    override val links: List<Link>,
) : BaseSchedule, Linkable {
    init {
        items.forEach { it.initData(columns) }
    }
}
