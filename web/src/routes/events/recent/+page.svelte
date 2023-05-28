<script lang="ts">
    import type {OrganizedEvent} from "vods.speedrun.club-client";
    import SmallEventSummary from "$lib/SmallEventSummary.svelte";
    import PageHeadTags from "$lib/PageHeadTags.svelte";

    export let data: { events: OrganizedEvent[] };
</script>

<svelte:head>
    <PageHeadTags
            title="Recent Events"
            description="List of past marathons." />
</svelte:head>

<h1>Recent Events</h1>



<ul>
    {#each data.events as event}
        <li>
            <SmallEventSummary org={event.organization} event={event} />
        </li>
    {/each}
</ul>