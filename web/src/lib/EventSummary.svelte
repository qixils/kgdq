<script lang="ts">
    import type {Event, IdentifiedOrganization} from 'vods.speedrun.club-client';
    import {Formatters} from "$lib/Formatters";

    export let org: IdentifiedOrganization;

    export let event: Event;

    let formatter = new Formatters(event.paypalCurrency);

</script>

<li class="event-summary">
    <h2>{event.name}</h2>
    <p>
        {#if event.startTime && event.endTime}
            { Formatters.date_hero(event.startTime) } - { Formatters.date_hero(event.endTime) }
        {:else if event.startTime}
            { Formatters.date_hero(event.startTime) }
        {/if}
    </p>

    {#if event.charityName}
        {#if event.timeStatus == "FINISHED" }
            <p>Raised { formatter.money(event.amount) } to benefit {event.charityName}.</p>
        {:else}
            <p>Benefiting {event.charityName}.</p>
        {/if}
    {/if}

    {#if event.startTime && event.endTime}
        <p><a href="/event/{org.id}/{event.short}">View schedule and VODs</a></p>
        <p><a href="{event.scheduleUrl}" target="_blank">Official schedule on the {org.shortName} website</a></p>
    {/if}
</li>




