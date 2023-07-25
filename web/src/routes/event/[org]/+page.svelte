<script lang="ts">
    import type {MarathonEvent, Organization} from "vods.speedrun.club-client";
    import {SvcClient} from "vods.speedrun.club-client";
    import {page} from "$app/stores";
    import {BASE_URL} from "$lib/kgdq";
    import {onMount} from "svelte";
    import EventSummary from "$lib/EventSummary.svelte";
    import LoadingButton from "$lib/LoadingButton.svelte";
    import ErrorReport from "$lib/ErrorReport.svelte";
    import {meta} from "../../../stores";

    export let data: { org: Organization };
    let events: MarathonEvent[];
    let error: Error;
    let SVC = new SvcClient(BASE_URL, fetch);

    onMount(async () => {
        try {
            events = (await SVC.getEvents($page.params.org.toLowerCase())).reverse();
        } catch (e) {
            error = e;
        }
    });

    $meta = {
        title: `${data.org.displayName} Events`,
        description: `List of events hosted by ${data.org.displayName}.`
    }
</script>

<h1>Events by { data.org.displayName }</h1>

{#if events === undefined && error === undefined}
    <LoadingButton />
{:else if events !== undefined}
    <ul class="events-list">
        {#each events as event}
            <EventSummary event={event} org={$page.params.org.toLowerCase()} />
        {/each}
    </ul>
{:else}
    <ErrorReport
            message="Failed to load events: {error.message}" />
{/if}
