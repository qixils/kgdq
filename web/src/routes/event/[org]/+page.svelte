<script lang="ts">
    import {Event, Organization, SvcClient} from "vods.speedrun.club-client";
    import {page} from "$app/stores";
    import {Formatters} from "$lib/Formatters";
    import {BASE_URL} from "$lib/kgdq";
    import {onMount} from "svelte";
    import EventSummary from "$lib/EventSummary.svelte";

    export let data: { org: Organization };
    let events: Event[];
    let error: Error;
    let SVC = new SvcClient(BASE_URL, fetch);

    let org = { ...data.org, id: $page.params.org };

    onMount(async () => {
        try {
            events = await SVC.getEvents($page.params.org);
        } catch (e) {
            error = e;
        }
    });
</script>

<svelte:head>
    <title>Speedrun VOD Club · { data.org.shortName } Events</title>
    <meta name="description" content="TODO KGDQ meta tags" />
</svelte:head>

<h1>Events by { data.org.displayName }</h1>

{#if events === undefined && error === undefined}
    <p>loading...</p>
{:else if events !== undefined}
    <ul class="org-events">
        {#each events as event}
            <EventSummary {event} {org} />
        {/each}
    </ul>
{:else}
    <p class="error">Error loading events: {error.message}</p>
{/if}
