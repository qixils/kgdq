<script lang="ts">
    import type { Event } from "../../../gdq";
    import SmallEventSummary from "$lib/SmallEventSummary.svelte";

    export let data: { events: [slug: string, Event][]};

</script>

<svelte:head>
    <title>Speedrun VOD Club - Upcoming Events</title>
    <meta name="description" content="TODO KGDQ meta tags" />
</svelte:head>

<h1>Current and Upcoming Events</h1>

<ul>
    {#each data.events as {slug, event}}
        <li>
            <SmallEventSummary  slug={slug} event={event} />
        </li>
    {/each}
</ul>