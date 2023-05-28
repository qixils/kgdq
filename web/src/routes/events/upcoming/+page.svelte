<script lang="ts">
    import type {OrganizedEvent} from "vods.speedrun.club-client";
    import SmallEventSummary from "$lib/SmallEventSummary.svelte";
    import PageHeadTags from "$lib/PageHeadTags.svelte";

    export let data: { events: OrganizedEvent[] };

    let current_events = data.events.filter(event => event.timeStatus === "IN_PROGRESS")
    let upcoming_events = data.events.filter(event => event.timeStatus === "UPCOMING");


</script>

<svelte:head>
    <PageHeadTags
            title="Upcoming Events"
            description="List of marathons that will happen in the future." />
</svelte:head>

{#if current_events.length === 0 && upcoming_events.length === 0}
    <p>There are no upcoming events.</p>
{/if}


{#if current_events.length > 0}
    <h1>Current Events</h1>
    <ul>
        {#each current_events as event}
            <li>
                <SmallEventSummary  org={event.organization} event={event} />
            </li>
        {/each}
    </ul>
{/if}

{#if upcoming_events.length > 0}
    <h1>Upcoming Events</h1>
    <ul>
        {#each upcoming_events as event}
            <li>
                <SmallEventSummary  org={event.organization} event={event} />
            </li>
        {/each}
    </ul>
{/if}