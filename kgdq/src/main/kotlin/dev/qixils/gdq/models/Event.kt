package dev.qixils.gdq.models

import dev.qixils.gdq.GDQ
import dev.qixils.gdq.ModelType
import dev.qixils.gdq.serializers.InstantAsStringSerializer
import dev.qixils.gdq.serializers.ZoneIdSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Serializable
data class Event(
    val short: String,
    val name: String,
    val hashtag: String = "#$short",
//    @SerialName("use_one_step_screening") val useOneStepScreening: Boolean, - some sort of internal variable. commenting out because it's not in ESA's API
    @SerialName("receivername") val receiverName: String,
    @SerialName("targetamount") val targetAmount: Float,
    @SerialName("minimumdonation") val minimumDonation: Float,
    @SerialName("paypalemail") val paypalEmail: String,
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
    // TODO: prize countries?
) : Model {

    override suspend fun loadData(api: GDQ, id: Int) {
        // datetime fallback
        if (_datetime == null)
            _datetime = api.query(type = ModelType.RUN, event = id)
                .minByOrNull { it.value.order }?.value?.startTime
                ?: throw IllegalStateException("Could not find the start time of the event")

        // canonical URL fallback
        if (_canonicalUrl == null)
            _canonicalUrl = api.apiPath.replaceFirst("/search/", "/index/", false) + short
    }

    val datetime: Instant get() = _datetime!!
    val canonicalUrl: String get() = _canonicalUrl!!
    val zonedDateTime: ZonedDateTime get() = datetime.atZone(timezone)

}