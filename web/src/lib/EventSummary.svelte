<script lang="ts">
    import type {MarathonEvent} from 'vods.speedrun.club-client';
    import {Formatters} from "$lib/Formatters";
    import {guessStreamNameAndUrl} from "$lib/kgdq";

    export let org: string;

    export let event: MarathonEvent = undefined as MarathonEvent;

    let formatter = new Formatters(event.paypalCurrency);

    const {streamName, streamUrl} = guessStreamNameAndUrl(org, event.short);
</script>

<li class="event-summary">
    <h2>{event.name}</h2>
    <p>
        {#if event.startTime && event.endTime && event.timeStatus === "FINISHED"}
            { Formatters.date_hero(event.startTime) } - { Formatters.date_hero(event.endTime) }.
        {:else if event.startTime && event.timeStatus === "UPCOMING"}
            Begins { Formatters.date_hero(event.startTime) }.
        {:else if event.endTime && event.timeStatus === "IN_PROGRESS"}
            Ends { Formatters.date_hero(event.endTime) }.
        {:else if event.startTime}
            { Formatters.date_hero(event.startTime) }.
        {/if}
    </p>

    {#if event.charityName}
        {#if event.timeStatus === "FINISHED" && event.amount > 0 }
            <p>Raised { formatter.money(event.amount) } to benefit {event.charityName}.</p>
        {:else}
            <p>Benefiting {event.charityName}.</p>
        {/if}
    {/if}

    {#if event.startTime && event.endTime}
        <a class="schedule-btn" href="/event/{org}/{event.short.toLowerCase()}">View schedule and VODs</a>
        <p>See also the <a href="{event.scheduleUrl}" target="_blank">official schedule on the {org.toUpperCase()} website ↗</a></p>
    {/if}

    {#if event.timeStatus === "IN_PROGRESS" && streamUrl && streamName}
        <p>Watch the event live: <a href="{streamUrl}" target="_blank">{streamName} ↗</a></p>
    {/if}
</li>




