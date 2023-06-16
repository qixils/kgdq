<script lang="ts">
    import type {MarathonEvent} from "vods.speedrun.club-client";
    import PageHeadTags from "$lib/PageHeadTags.svelte";
    import EventSummary from "$lib/EventSummary.svelte";
    import ErrorReport from "$lib/ErrorReport.svelte";

    export let data: { events: MarathonEvent[] | Error };
</script>

<svelte:head>
    <PageHeadTags
            title="Recent Events"
            description="List of past marathon events." />
</svelte:head>

<h1>Recent Events</h1>

{#if data.events instanceof Error}
    <ErrorReport
        message="Failed to load events: {data.events.message}" />
{:else}
    <ul class="events-list">
        {#each data.events as event}
            <EventSummary org={event.organization} event={event} />
        {/each}
    </ul>
{/if}

