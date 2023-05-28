<script lang="ts">
    import {Formatters} from "$lib/Formatters";
    import VODs from "$lib/VODs.svelte";
    import type {Run} from "vods.speedrun.club-client";
    import BidTarget from "$lib/BidTarget.svelte";
    import BidWar from "$lib/BidWar.svelte";
    import {fake_status} from "$lib/kgdq";
    import {user} from "../stores";
    import {page} from "$app/stores";
    import {svc} from "vods.speedrun.club-client";

    export let runs: Run[];
    export let run_index: number;
    export let formatter: Formatters;

    let run = runs[run_index];
    // TODO: step_color style var
    let step_color = run.timeStatus === "UPCOMING" ? "" : (run.timeStatus === "IN_PROGRESS" ? "#fff" : "#d2a");

    let previous_status = fake_status(run, run_index - 1);
    let current_status = fake_status(run, run_index);
    let next_status = fake_status(run, run_index + 1);

    async function suggest_navigate(org, event_slug, index) {
        let event_id = (await svc.getEvent(org, event_slug)).id;

        let navigate_query = new URLSearchParams();
        navigate_query.set("gdq_id", event_id.toString());
        navigate_query.set("horaro_id", index.toString());
        navigate_query.set("organization", org);

        let navigate_url = `/suggest?${navigate_query.toString()}`;

        window.location.href = navigate_url;
    }

</script>

<li class='run {
    current_status === "IN_PROGRESS" ? "in-progress" :
    current_status === "FINISHED" ? "finished" :
    current_status === "UPCOMING" && previous_status === "FINISHED" ? "next-up" :
    "" }'
    style="--schedule-color: { step_color }">
     <!-- later upcoming runs have no extra class -->
    <div class="schedule-bar-bit" ></div>
    <div class="run-schedule-time">{ Formatters.time(run.startTime) }</div>
    <div class="run-content">
        {#if $user }
            <button class="suggest-btn" on:click={ () => suggest_navigate($page.params.org, $page.params.slug, run.horaroId) }>Suggest VOD</button>
        {/if}
        <p>
            <b class="run-name">{run.name}</b>

            {#if run.category !== ""}
                <span class="run-category">{run.category}</span>
            {/if}
        </p>



        <div class="run-details">
            <div class="run-time">{ run.runTime }</div>
            {#if run.console !== "" }
                <div class="run-console">{ run.console }</div>
            {/if}
            <div class="run-runners {run.runners.length === 1 ? 'single' : 'multiple'}">
                <span>
                    {#each run.runners as runner, index}
                        {#if index > 0}
                            ,
                        {/if}
                        {#if runner.url !== null && runner.url !== undefined}
                            <a href={runner.url} target="_blank" rel="noopener noreferrer">{runner.name}</a>
                        {:else}
                            {runner.name}
                        {/if}
                    {/each}
                </span>
            </div>
            {#if run.src !== null && run.src !== undefined}
                <div class="run-src">
                    <a href="https://speedrun.com/{run.src}" target="_blank" rel="noopener noreferrer">Leaderboards</a>
                </div>
            {/if}
        </div>

        {#each run.bids as bid}
            <div class="bid">
                {#if bid.isTarget}
                    <BidTarget {bid} {formatter} />
                {:else}
                    <BidWar {bid} {formatter} />
                {/if}
            </div>
        {/each}

        {#if run.vods.length > 0}
            <VODs {run} />
        {/if}


    </div>
</li>
