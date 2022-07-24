package dev.qixils.gdq.models

import dev.qixils.gdq.GDQ
import dev.qixils.gdq.serializers.InstantSerializer
import dev.qixils.gdq.serializers.ZoneIdSerializer
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.Transient
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Serializable
data class Event(
    val short: String,
    val name: String,
    val hashtag: String,
    @SerialName("use_one_step_screening") val useOneStepScreening: Boolean,
    @SerialName("receivername") val receiverName: String,
    @SerialName("targetamount") val targetAmount: Float,
    @SerialName("minimumdonation") val minimumDonation: Float,
    @SerialName("paypalemail") val paypalEmail: String,
    @SerialName("paypalcurrency") val paypalCurrency: String,
    @Serializable(with = InstantSerializer::class) val datetime: Instant,
    @Serializable(with = ZoneIdSerializer::class) val timezone: ZoneId,
    val locked: Boolean,
    @SerialName("allow_donations") val allowDonations: Boolean,
    @SerialName("canonical_url") val canonicalUrl: String,
    val public: String,
    val amount: Float,
    val count: Int,
    val max: Float,
    val avg: Double,
    // TODO: prize countries?
) : Model {

    @Transient val zonedDateTime: ZonedDateTime = datetime.atZone(timezone)

}