<script lang="ts">
    import type {MarathonEvent} from "vods.speedrun.club-client";
    import PageHeadTags from "$lib/PageHeadTags.svelte";
    import EventSummary from "$lib/EventSummary.svelte";
    import ErrorReport from "$lib/ErrorReport.svelte";

    export let data: { events: MarathonEvent[] | Error };
</script>

<svelte:head>
    <PageHeadTags
            title="Upcoming Events"
            description="List of marathon events that will happen in the future." />
</svelte:head>

<h1>Upcoming Events</h1>

{#if data.events instanceof Error}
    <ErrorReport
            message="Failed to load events: {data.events.message}" />
{:else if data.events.length === 0}
    <p>There are no upcoming events.</p>
{:else}
    <ul class="events-list">
        {#each data.events as event}
            <EventSummary org={event.organization} event={event} />
        {/each}
    </ul>
{/if}