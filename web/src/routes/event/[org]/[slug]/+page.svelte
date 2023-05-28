<!-- TODO: support displaying multiple "events" (e.g. ESA Stream 1 & 2) on the same page -->

<script lang="ts">
    import {Event, Run, SvcClient} from 'vods.speedrun.club-client';
    import RunComponent from "$lib/RunComponent.svelte";
    import {page} from "$app/stores";
    import {Formatters} from "$lib/Formatters";
    import {BASE_URL, niceShortName} from "$lib/kgdq";
    import {onMount} from "svelte";
    import PageHeadTags from "$lib/PageHeadTags.svelte";
    import LoadingButton from "$lib/LoadingButton.svelte";

    export let data: { event: Event };
    let SVC = new SvcClient(BASE_URL, fetch);
    let event: Event = data.event; // slight backwards compat
    let formatter = new Formatters(event.paypalCurrency);
    let runs: Run[];
    let run_error: Error;

    onMount(async () => {
        try {
            runs = await SVC.getRuns($page.params.org, $page.params.slug);
        } catch (e) {
            run_error = e;
        }
    });
</script>

<svelte:head>
    <PageHeadTags
        title={ event.name }
        description="View the schedule of {niceShortName(event)} and watch back the runs." />
</svelte:head>

<section>
    <div class="event-description">
        <h1>{event.name}</h1>
        <p>
            {niceShortName(event)}

            {#if event.timeStatus === "UPCOMING"}
                will run from {Formatters.date_hero(event.startTime)} to {Formatters.date_hero(event.endTime)} and raise money
            {:else if event.timeStatus === "IN_PROGRESS"}
                is running from {Formatters.date_hero(event.startTime)} to {Formatters.date_hero(event.endTime)} and has raised {formatter.money(event.amount)}
            {:else}
                ran from {Formatters.date_hero(event.startTime)} to {Formatters.date_hero(event.endTime)} and raised {formatter.money(event.amount)}
            {/if}
            for
            {#if event.charityName !== ""}
                {event.charityName}.
            {:else}
                charity.
            {/if}
        </p>
        <p>Below you can find the schedule for the event and click on the play icon to the left of each run to watch back the run's VOD.</p>
        <!-- TODO: jump to current run button -->
    </div>

    {#if runs === undefined && run_error === undefined}
        <LoadingButton />
    {:else if runs !== undefined}
        <ul class="event-runs">

            {#each runs as run, run_index}

                { @const last_run = runs[run_index - 1] }
                { @const last_run_status = last_run?.timeStatus }
                { @const this_run_status = run.timeStatus }

                {#if run_index === 0 || new Date(run.startTime).getDay() !== new Date(last_run.startTime).getDay()}

                    <li class='event-day {
                        (this_run_status !== "UPCOMING" || last_run_status === "FINISHED") ? "finished" : ""
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
    {:else}
        <p class="error-box">Failed to load runs: {run_error.message}</p>
    {/if}
</section>
