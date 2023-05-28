<script lang="ts">
    import {Formatters} from "$lib/Formatters";
    import VODs from "$lib/VODs.svelte";
    import type {Run} from "vods.speedrun.club-client";
    import {svc} from "vods.speedrun.club-client";
    import BidTarget from "$lib/BidTarget.svelte";
    import BidWar from "$lib/BidWar.svelte";
    import {BASE_URL} from "$lib/kgdq";
    import {user} from "../stores";
    import {page} from "$app/stores";

    export let runs: Run[];
    export let run_index: number;
    export let formatter: Formatters;

    let run = runs[run_index];
    // TODO: step_color style var
    let step_color = run.timeStatus === "UPCOMING" ? "" : (run.timeStatus === "IN_PROGRESS" ? "#fff" : "#d2a");

    let previous_status = run_index === 0 ? null : runs[run_index - 1].timeStatus;
    let current_status = run.timeStatus
    let next_status = run_index === runs.length - 1 ? null : runs[run_index + 1].timeStatus;

    async function suggest_navigate(org, event_slug, index) {
        let event_id = (await svc.getEvent(org, event_slug)).id;

        let navigate_query = new URLSearchParams();
        navigate_query.set("gdq_id", event_id.toString());
        navigate_query.set("horaro_id", index.toString());
        navigate_query.set("organization", org);

        let navigate_url = `/suggest?${navigate_query.toString()}`;

        window.location.href = navigate_url;
    }

    let suggest_dialog = () => (document.getElementById(`suggest-${run_index}`) as HTMLDialogElement );

    let submit_status: string | null = null;

    async function submit_suggestion(e: Event) {
        e.preventDefault();
        let btn = document.getElementById(`suggest-${run_index}-btn`);
        btn.setAttribute("disabled", "");
        btn.blur();

        let url = (document.getElementById(`suggest-${run_index}-url`) as HTMLInputElement).value;

        submit_status = "SUBMITTING";

        let res = await fetch(`${BASE_URL}/suggest/vod`, {
            credentials: "include",
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                    url: url,
                    organization: $page.params.org,
                    gdqId: run.gdqId,
                    horaroId: run.horaroId
                })

        });

        console.log(res);
        let res_text = res.statusText + " " + await res.text();
        console.log(res_text)

        if (res.ok) {
            submit_status = "OK";
        } else {
            submit_status = "ERROR";
            alert("Error submitting VOD: " + res_text);
        }

        await new Promise(resolve => setTimeout(resolve, 2000));

        suggest_dialog().close();
        btn.removeAttribute("disabled");

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
        {#if $user && (current_status === "FINISHED" || current_status === "IN_PROGRESS") }
            <dialog id="suggest-{run_index}"  class="suggest-dialog { submit_status?.toLowerCase() ?? '' }">
                <button class="close-btn material-symbols-rounded" on:click={ () => suggest_dialog().close() }>close</button>
                <h1>Suggest a VOD</h1>

                <form>
                    <label for="suggest-{run_index}-url">URL</label>
                    <input type="text" id="suggest-{run_index}-url" name="url">

                    <label for="suggest-{run_index}-organization">Organization</label>
                    <input type="text" id="suggest-{run_index}-organization" name="organization" value="{ $page.params.org }" disabled>

                    <label for="suggest-{run_index}-gdqId" >GDQ ID</label>
                    <input type="text" id="suggest-{run_index}-gdqId" name="gdqId" value="{ run.gdqId }" disabled>

                    <label for="suggest-{run_index}-horaroId" >Horaro ID</label>
                    <input type="text" id="suggest-{run_index}-horaroId" name="horaroId" value="{ run.horaroId }" disabled>

                    <button id="suggest-{run_index}-btn" type="submit" on:click={ submit_suggestion }>
                        {#if submit_status === null}
                            Submit
                        {:else if submit_status === "SUBMITTING"}
                            Submitting...
                        {:else if submit_status === "OK"}
                            Submitted!
                        {:else if submit_status === "ERROR"}
                            Error!
                        {/if}
                    </button>
                </form>

            </dialog>

            <button class="suggest-btn" on:click={ () => suggest_dialog().showModal() }>
                Suggest VOD
            </button>
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
