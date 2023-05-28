<script lang="ts">
    import type {OrganizedEvent} from "vods.speedrun.club-client";
    import PageHeadTags from "$lib/PageHeadTags.svelte";
    import EventSummary from "$lib/EventSummary.svelte";

    export let data: { events: OrganizedEvent[] };

    let current_events = data.events.filter(event => event.timeStatus === "IN_PROGRESS")
    let upcoming_events = data.events.filter(event => event.timeStatus === "UPCOMING");


</script>

<svelte:head>
    <PageHeadTags
            title="Upcoming Events"
            description="List of marathons that will happen in the future." />
</svelte:head>

{#if current_events.length === 0 && upcoming_events.length === 0}
    <p>There are no upcoming events.</p>
{/if}


{#if current_events.length > 0}
    <h1>Current Events</h1>
    <ul class="events-list">
        {#each current_events as event}
            <EventSummary  org={event.organization} event={event} />
        {/each}
    </ul>
{/if}

{#if upcoming_events.length > 0}
    <h1>Upcoming Events</h1>
    <ul class="events-list">
        {#each upcoming_events as event}
            <EventSummary  org={event.organization} event={event} />
        {/each}
    </ul>
{/if}