package dev.qixils.gdq.v2.models

import dev.qixils.gdq.serializers.InstantAsStringSerializer
import dev.qixils.gdq.serializers.ZoneIdAsString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId

/**
 *       "type": "event",
 *       "id": 58,
 *       "short": "flamefatales2025",
 *       "name": "Flame Fatales 2025",
 *       "amount": 0.0,
 *       "donation_count": 0,
 *       "paypalcurrency": "USD",
 *       "hashtag": "FlameFatales",
 *       "datetime": "2025-09-07T12:30:00-04:00",
 *       "timezone": "US/Eastern",
 *       "receivername": "Malala Fund",
 *       "receiver_short": "",
 *       "receiver_solicitation_text": "",
 *       "receiver_logo": "",
 *       "receiver_privacy_policy": "",
 *       "use_one_step_screening": false,
 *       "locked": false,
 *       "archived": false,
 *       "draft": true,
 *       "allow_donations": false
 */

@Serializable
class Event(
    override val type: String = "event",
    override val id: Int,
    val short: String,
    val name: String,
    val hashtag: String = short,
    @Serializable(with = InstantAsStringSerializer::class) val datetime: Instant = Instant.MIN,
    val timezone: ZoneIdAsString = ZoneId.of("US/Eastern"),
    val locked: Boolean = false,
    val archived: Boolean = false,
    val draft: Boolean = false,
    @SerialName("allow_donations") val allowDonations: Boolean = false,
    @SerialName("receivername") val receiverName: String = "",
    @SerialName("receiver_short") val receiverShort: String = "",
    @SerialName("receiver_solicitation_text") val receiverSolicitationText: String = "",
    @SerialName("receiver_logo") val receiverLogo: String = "",
    @SerialName("receiver_privacy_policy") val receiverPrivacyPolicy: String = "",
    @SerialName("paypalcurrency") val paypalCurrency: String = "USD",
    val amount: Double = 0.0,
    @SerialName("donation_count") val donationCount: Int = 0,
) : TypedModel()