<script lang="ts">
    import type {OrganizedEvent} from "vods.speedrun.club-client";
    import PageHeadTags from "$lib/PageHeadTags.svelte";
    import EventSummary from "$lib/EventSummary.svelte";

    export let data: { events: OrganizedEvent[] };
</script>

<svelte:head>
    <PageHeadTags
            title="Upcoming Events"
            description="List of marathons that will happen in the future." />
</svelte:head>

{#if data.events.length === 0}
    <p>There are no upcoming events.</p>
{/if}

{#if data.events.length > 0}
    <h1>Upcoming Events</h1>
    <ul class="events-list">
        {#each data.events as event}
            <EventSummary  org={event.organization} event={event} />
        {/each}
    </ul>
{/if}