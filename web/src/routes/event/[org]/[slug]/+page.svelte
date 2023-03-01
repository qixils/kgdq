<!-- TODO: enable some preloading (mostly to init the head block server-side) -->
<!-- TODO: support displaying multiple "events" (e.g. ESA Stream 1 & 2) on the same page -->

<script lang="ts">
    import type {Event, Run} from 'src/gdq';
    import {page} from "$app/stores";
    import {Formatters} from "$lib/Formatters";

    let formatter = new Formatters("USD");

    let event_promise: Promise<Event> = fetch(`https://vods.speedrun.club/api/v2/marathons/${$page.params.org}/events?id=${$page.params.slug}`).then(r => {
        if (r.status !== 200) {
            return Promise.reject(new Error(`failed to fetch data (error ${r.status})`));
        }
        return r.json().then(r => {
            if (r.length === 0) {
                return Promise.reject(new Error("not found"));
            }
            let event: Event = r[0];
            formatter = new Formatters(event.paypalCurrency);
            return r[0];
        });
    });
    let runs_promise: Promise<Run[]> = fetch(`https://vods.speedrun.club/api/v2/marathons/${$page.params.org}/runs?event=${$page.params.slug}`).then(r => {
        if (r.status !== 200) {
            return Promise.reject(new Error(`failed to fetch data (error ${r.status})`));
        }
        return r.json();
    });
</script>

<svelte:head>
    <!-- TODO -->
</svelte:head>

<section>
    {#await event_promise}
        <div class="text-center my-3"><button class="btn loading">loading</button></div>
    {:then event}
        <div class="hero bg-base-200 mt-2">
            <div class="hero-content text-center">
                <div>
                    <h1 class="text-4xl font-bold">{event.name}</h1>
                    <p class="py-4">
                        {event.short.toUpperCase()} <!-- TODO: this looks bad for some events (namely ESA) -->
                        {#if event.timeStatus === "UPCOMING"}
                            will run from {Formatters.date_hero(event.startTime)} to {Formatters.date_hero(event.endTime)} and raise money
                        {:else if event.timeStatus === "IN_PROGRESS"}
                            is running from {Formatters.date_hero(event.startTime)} to {Formatters.date_hero(event.endTime)} and has raised {formatter.money(event.amount)}
                        {:else}
                            ran from {Formatters.date_hero(event.startTime)} to {Formatters.date_hero(event.endTime)} and raised {formatter.money(event.amount)}
                        {/if}
                        for {event.charityName}.
                    </p>
                    <p>
                        Below you can find the schedule for the event and click on the play icon to the left of each run
                        to watch back the run's VOD.
                    </p>
                </div>
            </div>
        </div>

        {#await runs_promise}
            <div class="text-center my-3"><button class="btn loading">loading</button></div>
        {:then runs}
            <ul class="steps steps-vertical block w-full max-w-screen-lg mx-auto overflow-x-hidden text-sm md:text-base">
                {#each runs as run, run_index}
                    <Run {runs} {run_index} {formatter} />
                {:else}
                    <!-- TODO: message noting no runs have been scheduled yet if in future, else schedule is unknown -->
                {/each}
            </ul>
        {:catch run_error}
            <p class="error">Error loading runs: {run_error.message}</p>
        {/await}
    {:catch event_error}
        <p class="error">Error loading event: {event_error.message}</p>
    {/await}
</section>
