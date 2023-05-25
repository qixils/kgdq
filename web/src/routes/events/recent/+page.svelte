<svelte:head>
    <title>Speedrun VOD Club - Recent Events</title>
    <meta name="description" content="TODO KGDQ meta tags" />
</svelte:head>

<h1>Recent Events</h1>

<script lang="ts">
    import type { Event } from "src/gdq";
    import SmallEventSummary from "$lib/SmallEventSummary.svelte";

    export let data: { events: {org: string, event: Event}[]};

</script>

<ul>
    {#each data.events as {org, event}}
        <li>
            <SmallEventSummary org={org} event={event} />
        </li>
    {/each}
</ul>