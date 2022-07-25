package dev.qixils.horaro.models

import kotlinx.serialization.Serializable

@Serializable
data class ResponseImpl<M>(
    override val data: M
) : Response<M>

// I can't be bothered to make some registry system for what's supposed to be a simple API
// So instead I'm hardcoding all the pagination and response implementations
// (...These implementations below technically aren't necessary, but they're a lot easier to read)

@Serializable
data class EventResponse(
    override val data: List<Event>,
    override val pagination: EventPaginator?,
) : ListResponse<Event, EventResponse>

@Serializable
data class ScheduleResponse(
    override val data: List<FullSchedule>,
    override val pagination: SchedulePaginator?,
) : ListResponse<FullSchedule, ScheduleResponse>
