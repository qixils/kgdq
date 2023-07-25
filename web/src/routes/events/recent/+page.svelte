<script lang="ts">
    import type {MarathonEvent} from "vods.speedrun.club-client";
    import EventSummary from "$lib/EventSummary.svelte";
    import ErrorReport from "$lib/ErrorReport.svelte";
    import {meta} from "../../../stores";

    export let data: { events: MarathonEvent[] | Error };

    $meta = {
        title: "Recent Events",
        description: "List of past marathon events."
    }
</script>

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

