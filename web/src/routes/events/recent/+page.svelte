<script lang="ts">
    import type {OrganizedEvent} from "vods.speedrun.club-client";
    import PageHeadTags from "$lib/PageHeadTags.svelte";
    import EventSummary from "$lib/EventSummary.svelte";

    export let data: { events: OrganizedEvent[] };
</script>

<svelte:head>
    <PageHeadTags
            title="Recent Events"
            description="List of past marathon events." />
</svelte:head>

<h1>Recent Events</h1>

<ul class="events-list">
    {#each data.events as event}
        <EventSummary org={event.organization} event={event} />
    {/each}
</ul>