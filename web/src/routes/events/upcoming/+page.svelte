<script lang="ts">
    import type { Event } from "src/gdq";
    import SmallEventSummary from "$lib/SmallEventSummary.svelte";

    export let data: { events: {org: string, event: Event}[]};

</script>

<svelte:head>
    <title>Speedrun VOD Club - Upcoming Events</title>
    <meta name="description" content="TODO KGDQ meta tags" />
</svelte:head>

<h1>Current and Upcoming Events</h1>

<ul>
    {#each data.events as {org, event}}
        <li>
            <SmallEventSummary  org={org} event={event} />
        </li>
    {/each}
</ul>