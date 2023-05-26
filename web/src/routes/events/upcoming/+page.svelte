<script lang="ts">
    import type {OrganizedEvent} from "vods.speedrun.club-client";
    import SmallEventSummary from "$lib/SmallEventSummary.svelte";

    export let data: { events: OrganizedEvent[] };
</script>

<svelte:head>
    <title>Speedrun VOD Club · Upcoming Events</title>
    <meta name="description" content="TODO KGDQ meta tags" />
</svelte:head>

<h1>Current and Upcoming Events</h1>

<ul>
    {#each data.events as event}
        <li>
            <SmallEventSummary  org={event.organization} event={event} />
        </li>
    {/each}
</ul>