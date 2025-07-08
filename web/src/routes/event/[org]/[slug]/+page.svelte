<!-- TODO: support displaying multiple "events" (e.g. ESA Stream 1 & 2) on the same page -->

<script lang="ts">
    import type {MarathonEvent, Run} from 'vods.speedrun.club-client';
    import {SvcClient} from 'vods.speedrun.club-client';
    import RunComponent from "$lib/RunComponent.svelte";
    import {page} from "$app/stores";
    import {Formatters} from "$lib/Formatters";
    import {WEBSITE_BASE, BASE_URL, guessStreamNameAndUrl, niceShortName} from "$lib/kgdq";
    import {onMount} from "svelte";
    import LoadingButton from "$lib/LoadingButton.svelte";
    import ErrorReport from "$lib/ErrorReport.svelte";
    import {meta} from "../../../../stores";

    export let data: { event: MarathonEvent } = undefined as never;
    let SVC = new SvcClient(BASE_URL, fetch);
    let formatter = new Formatters(data.event.currency);
    let runs: Run[];
    let run_error: Error;

    let current_run_index: number | null = null;

    const { streamUsername } = guessStreamNameAndUrl(data.event.organization, data.event.short);

    let hideBids = false;
    let saveHideBids = (x: boolean) => {
        localStorage.setItem("hideBids", JSON.stringify(x));
        hideBids = x;
    };
    let there_are_bids = false;

    let runs_ui_spans: { index: number; day_progess_class: string; day_text: string; run_indices: number[]; hide: boolean }[] = [];

    onMount(async () => {
        hideBids = JSON.parse(localStorage.getItem("hideBids") || "false");
        try {
            runs = await SVC.getRuns($page.params.org.toLowerCase(), $page.params.slug);
            if (runs.length > 0) {
                let current_run: Run | undefined = runs.find(run => run.timeStatus === "IN_PROGRESS" || run.timeStatus === "UPCOMING");
                if (current_run !== undefined) {
                    current_run_index = runs.indexOf(current_run);
                }

            }
            let runs_ui_span_index = 0;
            for (let run_index = 0; run_index < runs.length; ++run_index) {
                let run = runs[run_index];

                if (run.bids.length > 0) {
                    there_are_bids = true;
                }

                let last_run = runs[run_index - 1];
                let last_run_status = last_run?.timeStatus;
                let this_run_status = run.timeStatus;
                if (run_index === 0 || new Date(run.startTime).getDay() !== new Date(last_run.startTime).getDay()) {
                    //  fill line for day if the next displayed run is at least next up:
                    let day_progess_class = (this_run_status !== "UPCOMING" || last_run_status === "FINISHED") ? "finished" : "";
                    let day_text = Formatters.date_weekday_date(run.startTime);
                    runs_ui_spans.push({
                        index: runs_ui_span_index,
                        day_progess_class,
                        day_text,
                        run_indices: <number[]>[],
                        hide: false,
                    })
                    ++runs_ui_span_index;
                }
                runs_ui_spans[runs_ui_spans.length-1].run_indices.push(run_index);
            }
            
        } catch (e) {
            run_error = e;
        }
    });

    $meta = {
        title: data.event.name,
        description: `View the schedule of ${niceShortName(data.event)} and watch back the VODs.`,
        url: `${WEBSITE_BASE}/event/${data.event.organization}/${data.event.short}`.toLowerCase()
    }
</script>

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
        {#if data.event.charityName}
            {data.event.charityName}.
        {:else}
            charity.
        {/if}
    </p>
    
    {#if data.event.timeStatus === "IN_PROGRESS" && streamUsername}
        <!-- TODO: `allow: autoplay;` search param -->
        <iframe src="https://player.twitch.tv/?channel={streamUsername}&parent={$page.url.hostname}"
                title="Stream"
                allow="fullscreen"
                style="
                    display: block;
                    width: calc(100% - 4px - 10px);
                    aspect-ratio: 16/9;
                    border: 2px solid #9146ff;
                    border-radius: 5px;
                    margin: 0 5px;
                    margin-bottom: 15px;
                ">
        </iframe>
    {/if}
    
</div>

<div id="event-controls" style="position: {current_run_index !== null || there_are_bids ? 'sticky' : 'intial'}">
    {#if current_run_index !== null}
        <a href="#run-{current_run_index}">Jump to current run</a>
    {/if}
    {#if there_are_bids}
        <div style="margin-left: auto">
            <input id="event-bids" type="checkbox" on:change={evt => saveHideBids(evt.currentTarget.checked)} checked={hideBids}>
            <label for="event-bids">Hide bids</label>
        </div>
    {/if}
</div>

{#if runs === undefined && run_error === undefined}
    <LoadingButton />
{:else if runs !== undefined}
    <ul class="event-runs" class:hide-bids={hideBids}>

        {#each runs_ui_spans as span, span_index}
            {@const prog = span.day_progess_class }
            {@const next_prog = runs_ui_spans[span_index+1]?.day_progess_class }
            <li class='event-day {prog == 'finished' && next_prog == '' && span.hide ? 'in-progress' : prog}'>
                    <div class="schedule-bar-bit"></div>
                <h2>
                    <button class="collapse-btn" on:click={ evt => {
                        span.hide = !span.hide;
                    } } >
                            <span class="material-symbols-rounded" style="font-size: 20pt; color: var(--text-quiet)" >
                            {span.hide ? 'expand_all' : 'collapse_all'}
                        </span>
                        { span.day_text }
                    </button>
                </h2>
                
                </li>
            {#if !span.hide }
                {#each span.run_indices as run_index}
                    <RunComponent {runs} {run_index} {formatter} />
                {/each}
            {/if}
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