package dev.qixils.gdq.models

import dev.qixils.gdq.GDQ
import dev.qixils.gdq.serializers.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Serializable
data class Event(
    val short: String,
    val name: String,
    val hashtag: String = "#$short",
//    @SerialName("use_one_step_screening") val useOneStepScreening: Boolean, - some sort of internal variable. commenting out because it's not in ESA's API
    @SerialName("receivername") val charityName: String,
    @Serializable(with = DoubleOrNoneNullSerializer::class) @SerialName("targetamount") val targetAmount: Double?,
    @Serializable(with = DoubleOrNoneNullSerializer::class) @SerialName("minimumdonation") val minimumDonation: Double?,
//    @SerialName("paypalemail") val paypalEmail: String, - don't see a reason to expose this
    @SerialName("paypalcurrency") val paypalCurrency: String,
    @SerialName("datetime") @Serializable(with = InstantAsStringSerializer::class) private var _starttime: Instant? = null,
    @SerialName("endtime") @Serializable(with = InstantAsStringSerializer::class) private var _endtime: Instant? = null,
    @Serializable(with = ZoneIdSerializer::class) val timezone: ZoneId,
    val locked: Boolean,
    @SerialName("allow_donations") val allowDonations: Boolean = !locked,
    @SerialName("canonical_url") private var _canonicalUrl: String? = null,
    val public: String,
    @Serializable(with = DoubleOrNoneZeroSerializer::class) val amount: Double,
    @Serializable(with = IntOrNoneZeroSerializer::class) val count: Int,
    @Serializable(with = DoubleOrNoneZeroSerializer::class) val max: Double,
    @Serializable(with = DoubleOrNoneZeroSerializer::class) val avg: Double,
    @SerialName("horaro_name") private val horaroName: String? = null,
) : Model {

    @Transient private var skipLoad = false
    override fun skipLoad() {
        skipLoad = true
    }
    override val skippedLoad: Boolean get() = skipLoad

    override suspend fun loadData(api: GDQ, id: Int) {
        // datetime fallback
        if (_starttime == null || _endtime == null) {
            api.updateEvent(id, skipLoad)
            if (_starttime == null)
                _starttime = api.eventStartedAt[id]
            if (_endtime == null)
                _endtime = api.eventEndedAt[id]
        }

        // canonical URL fallback
        if (_canonicalUrl == null)
            _canonicalUrl = api.apiPath.replaceFirst("/search/", "/index/", false) + short
    }

    /**
     * The [Instant] at which the event will start.
     *
     * @see zonedStartTime
     */
    val startTime: Instant? get() = _starttime

    /**
     * The [Instant] at which the event will end.
     *
     * @see zonedEndTime
     */
    val endTime: Instant? get() = _endtime

    /**
     * The [ZonedDateTime] at which the event will start.
     *
     * @see startTime
     */
    val zonedStartTime: ZonedDateTime? get() = startTime?.atZone(timezone)

    /**
     * The [ZonedDateTime] at which the event will end.
     *
     * @see endTime
     */
    val zonedEndTime: ZonedDateTime? get() = endTime?.atZone(timezone)

    /**
     * The public-facing URL of the event from the donation tracker website.
     */
    val canonicalUrl: String get() = _canonicalUrl!!

    @Transient private val horaroNameSplit = horaroName?.split("/") ?: emptyList()

    /**
     * The slug of ESA's Horaro event.
     */
    @Transient val horaroEvent: String? = if (horaroNameSplit.size == 2) horaroNameSplit[0] else null

    /**
     * The slug of this marathon's Horaro schedule.
     */
    @Transient val horaroSchedule: String? = if (horaroNameSplit.size == 2) horaroNameSplit[1] else null
}