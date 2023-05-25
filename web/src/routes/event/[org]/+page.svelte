<script lang="ts">
    import { fetchEvents } from "$lib/kgdq";
    import {page} from "$app/stores";
    import {Formatters} from "$lib/Formatters";

    let org_names = {
      gdq: 'Games Done Quick',
    };

    let org_name = org_names[$page.params.org];

</script>



<h1>Events by { org_name }</h1>

{#await fetchEvents($page.params.org)}
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

            <a href="{ event.canonicalUrl }" target="_blank">Event website on the { org_name } website</a>
        </div>
    {/each}
{:catch error}
    <p>error: {error.message}</p>
{/await}

