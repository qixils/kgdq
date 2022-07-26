package dev.qixils.horaro.models

import dev.qixils.gdq.serializers.DurationAsSecondsSerializer
import dev.qixils.gdq.serializers.OffsetDateTimeSerializer
import dev.qixils.horaro.InternalHoraroApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Duration
import java.time.OffsetDateTime

/**
 * An entry in a marathon's schedule.
 *
 * @param length    the duration of the run
 * @param scheduled the scheduled start time of the run
 * @param rawData   custom data values corresponding to the [BaseSchedule.columns] field
 */
@Serializable
data class Run(

    /**
     * The duration of the run.
     */
    @Serializable(with = DurationAsSecondsSerializer::class) @SerialName("length_t") val length: Duration,

    /**
     * The scheduled start time of the run.
     */
    @Serializable(with = OffsetDateTimeSerializer::class) val scheduled: OffsetDateTime,
    @SerialName("data") private val rawData: List<String?>,
) {
    @Transient private var _data: List<Datum>? = null

    /**
     * Initializes the custom data values.
     * For internal use only.
     *
     * @param columns the schedule's custom data columns
     */
    @InternalHoraroApi
    fun initData(columns: List<String>) {
        _data = rawData.mapIndexed { i, value -> Datum(columns[i], value) }
    }

    /**
     * The custom data values.
     */
    val data: List<Datum> get() = _data ?: throw IllegalStateException("Data not initialized")

    /**
     * Fetches a custom [data] value given its [column] name.
     *
     * @param column     the column name
     * @param ignoreCase whether to ignore case when searching for the column
     * @return the value or null if not found
     */
    fun getValue(column: String, ignoreCase: Boolean = true): String?
    = data.firstOrNull { column.equals(it.column, ignoreCase) }?.value
}

data class Datum(
    val column: String,
    val value: String?,
)
