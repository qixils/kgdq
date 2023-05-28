<script lang="ts">
    import {Event, Organization, SvcClient} from "vods.speedrun.club-client";
    import {page} from "$app/stores";
    import {BASE_URL} from "$lib/kgdq";
    import {onMount} from "svelte";
    import EventSummary from "$lib/EventSummary.svelte";
    import PageHeadTags from "$lib/PageHeadTags.svelte";
    import LoadingButton from "$lib/LoadingButton.svelte";
    import ErrorReport from "$lib/ErrorReport.svelte";

    export let data: { org: Organization };
    let events: Event[];
    let error: Error;
    let SVC = new SvcClient(BASE_URL, fetch);

    onMount(async () => {
        try {
            events = (await SVC.getEvents($page.params.org)).reverse();
        } catch (e) {
            error = e;
        }
    });
</script>

<svelte:head>
    <PageHeadTags
        title="{data.org.displayName} Events"
        description="List of events hosted by {data.org.displayName}." />
</svelte:head>

<h1>Events by { data.org.displayName }</h1>

{#if events === undefined && error === undefined}
    <LoadingButton />
{:else if events !== undefined}
    <ul class="events-list">
        {#each events as event}
            <EventSummary event={event} org={$page.params.org} />
        {/each}
    </ul>
{:else}
    <ErrorReport
            message="Failed to load events: {error.message}" />
{/if}
