<!-- TODO: support displaying multiple "events" (e.g. ESA Stream 1 & 2) on the same page -->

<script lang="ts">
    import type {MarathonEvent, Run} from 'vods.speedrun.club-client';
    import {SvcClient} from 'vods.speedrun.club-client';
    import RunComponent from "$lib/RunComponent.svelte";
    import {page} from "$app/stores";
    import {Formatters} from "$lib/Formatters";
    import {BASE_URL, niceShortName} from "$lib/kgdq";
    import {onMount} from "svelte";
    import PageHeadTags from "$lib/PageHeadTags.svelte";
    import LoadingButton from "$lib/LoadingButton.svelte";
    import ErrorReport from "$lib/ErrorReport.svelte";

    export let data: { event: MarathonEvent } = undefined as never;
    let SVC = new SvcClient(BASE_URL, fetch);
    let formatter = new Formatters(data.event.paypalCurrency);
    let runs: Run[];
    let run_error: Error;

    let current_run_index: number | null = null;

    onMount(async () => {
        try {
            runs = await SVC.getRuns($page.params.org.toLowerCase(), $page.params.slug);
            if (runs.length > 0) {
                let current_run: Run | undefined = runs.find(run => run.timeStatus === "IN_PROGRESS" || run.timeStatus === "UPCOMING");
                if (current_run !== undefined) {
                    current_run_index = runs.indexOf(current_run);
                }
            }
        } catch (e) {
            run_error = e;
        }
    });
</script>

<svelte:head>
    <PageHeadTags
        title={ data.event.name }
        description="View the schedule of {niceShortName(data.event)} and watch back the VODs." />
</svelte:head>

<section>
    <div class="event-description">
        <h1>{data.event.name}</h1>
        <p>
            {niceShortName(data.event)}

            {#if data.event.timeStatus === "UPCOMING"}
                will run from {Formatters.date_hero(data.event.startTime)} to {Formatters.date_hero(data.event.endTime)} and raise money
            {:else if data.event.timeStatus === "IN_PROGRESS"}
                is running from {Formatters.date_hero(data.event.startTime)} to {Formatters.date_hero(data.event.endTime)} and has raised {formatter.money(data.event.amount)}
            {:else}
                ran from {Formatters.date_hero(data.event.startTime)} to {Formatters.date_hero(data.event.endTime)} and raised {formatter.money(data.event.amount)}
            {/if}
            for
            {#if data.event.charityName !== ""}
                {data.event.charityName}.
            {:else}
                charity.
            {/if}
        </p>
        {#if current_run_index !== null}
            <p><a href="#run-{current_run_index}">Jump to current run</a></p>
        {/if}
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
                    {#if data.event.timeStatus === "UPCOMING"}
                        No runs have been scheduled for this event yet.
                    {:else}
                        No runs were found for this event.
                    {/if}
                </p>
            {/each}
        </ul>
    {:else}
        <ErrorReport
            message="Failed to load runs: {run_error.message}" />
    {/if}
</section>
