package club.speedrun.vods.marathon

import dev.qixils.gdq.serializers.InstantAsStringSerializer
import dev.qixils.gdq.serializers.ZoneIdSerializer
import dev.qixils.horaro.Horaro
import dev.qixils.horaro.models.FullSchedule
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant
import java.time.ZoneId

@Serializable
data class EventData(
    @Transient
    val organization: OrganizationConfig = EmptyOrganizationConfig,
    val id: Int,
    val short: String,
    val name: String,
    val hashtag: String,
    val charityName: String,
    val targetAmount: Double?,
    val minimumDonation: Double?,
    val paypalCurrency: String,
    @Serializable(with = InstantAsStringSerializer::class)
    val startTime: Instant?,
    @Serializable(with = InstantAsStringSerializer::class)
    val endTime: Instant?,
    @Serializable(with = ZoneIdSerializer::class)
    val timezone: ZoneId,
    val locked: Boolean,
    val allowDonations: Boolean,
    val canonicalUrl: String,
    val public: String,
    val amount: Double,
    val count: Int,
    val max: Double,
    val avg: Double,
    var horaroEvent: String? = null,
    var horaroSchedule: String? = null,
) {
    suspend fun horaroSchedule(): FullSchedule? {
        if (horaroEvent == null || horaroSchedule == null) return null
        return Horaro.getSchedule(horaroEvent!!, horaroSchedule!!)
    }

    val horaroUrl: String?
        = if (horaroEvent == null || horaroSchedule == null) null
        else "https://horaro.org/$horaroEvent/$horaroSchedule"
    val timeStatus: TimeStatus?

    // TODO: does this work in api-client ??
    val donationUrl: String = organization.getDonationUrl(this)
    val scheduleUrl: String = organization.getScheduleUrl(this)

    init {
        val now = Instant.now()
        timeStatus = when {
            startTime == null -> null
            now < startTime -> TimeStatus.UPCOMING
            endTime == null -> TimeStatus.FINISHED
            now < endTime -> TimeStatus.IN_PROGRESS
            else -> TimeStatus.FINISHED
        }
    }
}