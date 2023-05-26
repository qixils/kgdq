<script lang="ts">
    import {Event, Organization, SvcClient} from "vods.speedrun.club-client";
    import {page} from "$app/stores";
    import {Formatters} from "$lib/Formatters";
    import {BASE_URL} from "$lib/kgdq";
    import {onMount} from "svelte";

    export let data: { org: Organization };
    let events: Event[];
    let error: Error;
    let SVC = new SvcClient(BASE_URL, fetch);

    onMount(async () => {
        try {
            events = await SVC.getEvents($page.params.org);
        } catch (e) {
            error = e;
        }
    });
</script>

<h1>Events by { data.org.displayName }</h1>

{#if events === undefined && error === undefined}
    <p>loading...</p>
{:else if events !== undefined}
    {#each events as event}
        <div>
            <h2>{event.name}</h2>
            <p>
            {#if event.startTime && event.endTime}
                { Formatters.date_hero(event.startTime) } - { Formatters.date_hero(event.endTime) }
            {:else if event.startTime}
                { Formatters.date_hero(event.startTime) }
            {/if}
            </p>
            {#if event.charityName}
                <p>Benefiting {event.charityName}</p>
            {/if}

            <p><a href="/event/{$page.params.org}/{event.short}">View event schedule and VODs</a></p>
            {#if event.startTime && event.endTime} <!-- if event has times then it has runs and thus probably has a schedule -->
                <p><a href={event.scheduleUrl} target="_blank">View official schedule on the {data.org.shortName} website</a></p>
            {/if}
        </div>
    {/each}
{:else}
    <p class="error">Error loading events: {error.message}</p>
{/if}
