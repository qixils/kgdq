package dev.qixils.gdq.models

import dev.qixils.gdq.GDQ
import dev.qixils.gdq.ModelType
import dev.qixils.gdq.serializers.InstantAsStringSerializer
import dev.qixils.gdq.serializers.ZoneIdSerializer
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
    @SerialName("targetamount") val targetAmount: Float,
    @SerialName("minimumdonation") val minimumDonation: Float,
//    @SerialName("paypalemail") val paypalEmail: String, - don't see a reason to expose this
    @SerialName("paypalcurrency") val paypalCurrency: String,
    @SerialName("datetime") @Serializable(with = InstantAsStringSerializer::class) private var _datetime: Instant? = null,
    @Serializable(with = ZoneIdSerializer::class) val timezone: ZoneId,
    val locked: Boolean,
    @SerialName("allow_donations") val allowDonations: Boolean = !locked,
    @SerialName("canonical_url") private var _canonicalUrl: String? = null,
    val public: String,
    val amount: Float,
    val count: Int,
    val max: Float,
    val avg: Double,
    @SerialName("horaro_name") private val horaroName: String? = null,
) : Model {

    override suspend fun loadData(api: GDQ, id: Int) {
        // datetime fallback
        if (_datetime == null) {
            if (!api.eventStartedAt.containsKey(id))
                api.eventStartedAt[id] = api.query(type = ModelType.RUN, event = id)
                    .minByOrNull { it.value.order }?.value?.startTime
                    ?: Instant.EPOCH
            _datetime = api.eventStartedAt[id]
        }

        // canonical URL fallback
        if (_canonicalUrl == null)
            _canonicalUrl = api.apiPath.replaceFirst("/search/", "/index/", false) + short
    }

    /**
     * The [Instant] at which the event will start.
     *
     * @see zonedDateTime
     */
    val datetime: Instant get() = _datetime!!

    /**
     * The [ZonedDateTime] at which the event will start.
     *
     * @see datetime
     */
    val zonedDateTime: ZonedDateTime get() = datetime.atZone(timezone)

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