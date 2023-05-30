<script lang="ts">
    import type {Event} from 'vods.speedrun.club-client';
    import {Formatters} from "$lib/Formatters";

    export let org: string;

    export let event: Event = undefined as Event;

    let formatter = new Formatters(event.paypalCurrency);

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
        <p><a href="/event/{org}/{event.short}">View schedule and VODs</a></p>
        <p><a href="{event.scheduleUrl}" target="_blank">View official schedule on the {org.toUpperCase()} website</a></p>
    {/if}
</li>




