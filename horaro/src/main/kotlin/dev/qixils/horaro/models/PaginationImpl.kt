package dev.qixils.horaro.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

// I can't be bothered to make some registry system for what's supposed to be a simple API
// So instead I'm hardcoding all the pagination and response implementations

@Serializable
data class EventPaginator(
    override val offset: Int,
    override val max: Int,
    override val size: Int,
    override val links: List<Link>,
    override val serializer: KSerializer<EventResponse> = EventResponse.serializer()
) : Pagination<EventResponse>

@Serializable
data class SchedulePaginator(
    override val offset: Int,
    override val max: Int,
    override val size: Int,
    override val links: List<Link>,
    override val serializer: KSerializer<ScheduleResponse> = ScheduleResponse.serializer()
) : Pagination<ScheduleResponse>