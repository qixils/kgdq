<script lang="ts">
    import {Organization, SvcClient} from "vods.speedrun.club-client";
    import {page} from "$app/stores";
    import {Formatters} from "$lib/Formatters";

    const svc = new SvcClient();
    export let data: { org: Organization };
</script>



<h1>Events by { data.org.displayName }</h1>

{#await svc.getEvents($page.params.org)}
    <p>loading...</p>
{:then events}
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

            <a href="{event.scheduleUrl}" target="_blank">Official schedule on the {data.org.shortName} website</a>
        </div>
    {/each}
{:catch error}
    <p>error: {error.message}</p>
{/await}

