<svelte:head>
    <title>Speedrun VOD Club - Recent Events</title>
    <meta name="description" content="TODO KGDQ meta tags" />
</svelte:head>

<h1>Recent Events</h1>

<script lang="ts">
    import type { Event } from "../../../gdq";
    import SmallEventSummary from "$lib/SmallEventSummary.svelte";

    export let data: { events: [slug: string, Event][]};

</script>

<ul>
    {#each data.events as {slug, event}}
        <li>
            <SmallEventSummary  slug={slug} event={event} />
        </li>
    {/each}
</ul>