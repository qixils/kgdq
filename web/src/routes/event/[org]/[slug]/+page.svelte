<!-- TODO: support displaying multiple "events" (e.g. ESA Stream 1 & 2) on the same page -->

<script lang="ts">
    import type {Event, Run} from 'src/gdq';
    import RunComponent from "$lib/RunComponent.svelte";
    import {page} from "$app/stores";
    import {Formatters} from "$lib/Formatters";
    import {fake_status} from "$lib/kgdq";

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
            console.log(event);
            return r[0];
        });
    });
    let runs_promise: Promise<Run[]> = fetch(`https://vods.speedrun.club/api/v2/marathons/${$page.params.org}/runs?event=${$page.params.slug}`).then(r => {
        if (r.status !== 200) {
            return Promise.reject(new Error(`failed to fetch data (error ${r.status})`));
        }
        return r.json();
    });

    function niceShortName(event: Event) {
        let ESA_RE = /^esa([sw])(\d+)(?:s(\d+))?$/i;
        let match = event.short.match(ESA_RE);
        if (match) {
            let [_, season, year, stream_number] = match;
            let season_nice = season === "s" ? "Summer" : "Winter";
            if (stream_number) {
                return `ESA ${season_nice} ${year} S${stream_number}`;
            }
            return `ESA ${season_nice} ${year}`;
        }
        return event.short.toUpperCase();
    }

</script>

<svelte:head>
    <!-- TODO: add head -->
    <!-- also TODO: enable some ~~preloading~~ server-side rendering to init this server-side for SEO and embeds and whatnot -->
</svelte:head>

<section>
    {#await event_promise}
        <div class="loading"></div>
    {:then event}
        <div class="event-description">
            <h1>{ event.name }</h1>
            <p>
                { niceShortName(event) }

                {#if event.timeStatus === "UPCOMING"}
                    will run from {Formatters.date_hero(event.startTime)} to {Formatters.date_hero(event.endTime)} and raise money
                {:else if event.timeStatus === "IN_PROGRESS"}
                    is running from {Formatters.date_hero(event.startTime)} to {Formatters.date_hero(event.endTime)} and has raised {formatter.money(event.amount)}
                {:else}
                    ran from {Formatters.date_hero(event.startTime)} to {Formatters.date_hero(event.endTime)} and raised {formatter.money(event.amount)}
                {/if}
                for {event.charityName}.
            </p>
            <p>Below you can find the schedule for the event and click on the play icon to the left of each run to watch back the run's VOD</p>
        </div>

        {#await runs_promise}
            <div class="loading"></div>
        {:then runs}
            <ul class="event-runs">

                {#each runs as run, run_index}

                    <script>
                    /*
                        Possible states for schedule bar bit:
                        - First day marker, not yet that day
                        - First day marker, that day but not yet started first run
                        - First day marker, that day and started
                        - Run, not yet started and the previous run is not finished
                        - Run, not yet started and the previous run is finished (intermission)
                        - Run, started and not yet finished
                        - Run, finished
                        - Day marker, not yet that day but previous run is in progress and will finish after midnight
                        - Day marker, not yet that day but previous run is finished (before midnight)
                        - Day marker, not yet that day
                        - Day marker, that day but not yet started
                        - Day marker, that day and started first run of the day

                        Key predicates:
                    */
                    </script>

                    { @const last_run = runs[run_index - 1] }
                    { @const last_run_status = fake_status(last_run, run_index - 1) }
                    { @const this_run_status = fake_status(run, run_index ) }
                    <!--{ @const is_today = true }-->

                    {#if run_index === 0 || new Date(run.startTime).getDay() !== new Date(last_run.startTime).getDay()}

                        <li class='event-day {
                            (this_run_status != "UPCOMING" || last_run_status === "FINISHED") ? "finished" : ""
                            }'> <!-- fill line for day if the next displayed run is at least next up  -->
                            <div class="schedule-bar-bit"></div>
                            <h2>{ Formatters.date_weekday_date(run.startTime) }</h2>
                        </li>
                    {/if}
                    <RunComponent {runs} {run_index} {formatter} />
                {:else}
                    <p>
                        {#if event.timeStatus === "UPCOMING"}
                            No runs have been scheduled for this event yet.
                        {:else}
                            No runs were found for this event.
                        {/if}
                    </p>
                {/each}
            </ul>
        {:catch run_error}
            <p class="error">Error loading runs: {run_error.message}</p>
        {/await}
    {:catch event_error}
        <p class="error">Error loading event: {event_error.message}</p>
    {/await}
</section>
