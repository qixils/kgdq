<script lang="ts">
    import {Formatters} from "$lib/Formatters";
    import VODs from "$lib/VODs.svelte";
    import type {Run} from "vods.speedrun.club-client";
    import BidTarget from "$lib/BidTarget.svelte";
    import BidWar from "$lib/BidWar.svelte";
    import {BASE_URL} from "$lib/kgdq";
    import {user} from "../stores";
    import {page} from "$app/stores";

    export let run: Run = undefined as unknown as Run;
    export let runs: Run[] = undefined as unknown as Run[];
    export let run_index: number = undefined as unknown as number;
    export let formatter: Formatters = undefined as unknown as Formatters;

    // opt for a higher resolution cover if there is no keyart (since it will be reused), else lower
    $: cover_url = run.igdb?.cover
        ? run.igdb.background
            ? `https://images.igdb.com/igdb/image/upload/t_cover_small/${run.igdb?.cover}.jpg`
            : `https://images.igdb.com/igdb/image/upload/t_cover_big_2x/${run.igdb?.cover}.jpg`
        : '';
    $: keyart_url = 
        run.igdb?.background ? `https://images.igdb.com/igdb/image/upload/t_screenshot_med/${run.igdb?.background}.jpg`
        : cover_url;

    // console.log(`run # ${run_index}: ${run.igdb?.cover} / ${run.igdb?.background}`)

    $: previous_status = run_index === 0 ? null : runs[run_index - 1].timeStatus;
    $: current_status = run.timeStatus;
    $: next_status = run_index === runs.length - 1 ? null : runs[run_index + 1].timeStatus;

    if (previous_status === "IN_PROGRESS" && current_status === "IN_PROGRESS") {
        previous_status = "FINISHED";
        current_status = "UPCOMING";
    }
    if (current_status === "IN_PROGRESS" && next_status === "IN_PROGRESS") {
        previous_status = "FINISHED";
        current_status = "FINISHED";
    }

    // TODO: step_color style var
    $: step_color = run.timeStatus === "UPCOMING" ? "" : (current_status === "IN_PROGRESS" ? "#fff" : "#d2a");

    $: suggest_dialog = () => (document.getElementById(`suggest-${run_index}`) as HTMLDialogElement );

    let submit_status: "SUBMITTING" | "OK" | "ERROR" | null = null;

    async function submit_suggestion(e: Event) {
        e.preventDefault();
        let btn = document.getElementById(`suggest-${run_index}-btn`);
        if (!btn) return;

        btn.setAttribute("disabled", "");
        btn.blur();

        let url = (document.getElementById(`suggest-${run_index}-url`) as HTMLInputElement).value;

        // null: not an admin (no replacement)
        // "": no replacement
        // "<url>": replace
        let replace_url = (document.getElementById(`suggest-${run_index}-replace`) as HTMLSelectElement | null)?.value ?? null;

        submit_status = "SUBMITTING";

        if (replace_url) {
            let delete_response = await fetch(`${BASE_URL}/suggest/vod?url=${encodeURIComponent(replace_url)}`, {
                credentials: "include",
                method: "DELETE"
            });
            if (!delete_response.ok) {
                submit_status = "ERROR";
                alert("Error deleting existing suggestion: " + delete_response);
            }
        }

        if (submit_status !== "ERROR") {

            let post_response = await fetch(`${BASE_URL}/suggest/vod`, {
                credentials: "include",
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    url: url,
                    organization: $page.params.org.toLowerCase(),
                    id: run.id,
                })
            });

            console.log(post_response);
            let res_text = post_response.statusText + " " + await post_response.text();
            console.log(res_text)

            if (post_response.ok) {
                submit_status = "OK";
            } else {
                submit_status = "ERROR";
                alert("Error submitting VOD: " + res_text);
            }

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
    "" }
    {cover_url != '' ? "has-art": ""}
    '
    id="run-{ run_index }"
    style="--schedule-color: { step_color }; --keyart: url('{ keyart_url}')"
    >
     <!-- later upcoming runs have no extra class -->
    <div class="schedule-bar-bit" ></div>
    <div class="run-schedule-time">{ Formatters.time(run.startTime) }</div>
    <div class="run-cover {$user && (current_status === "FINISHED" || current_status === "IN_PROGRESS") && (run.id !== null) ? 'evade' : ''}">
            {#if run.igdb?.cover != null}
                <img src="{cover_url}">
            {/if}
        </div>
    <div class="run-content" 
        >
        
        {#if $user && (current_status === "FINISHED" || current_status === "IN_PROGRESS") && (run.id !== null) }
            <dialog id="suggest-{run_index}" class="suggest-dialog">
                <button class="close-btn material-symbols-rounded" on:click={ () => suggest_dialog().close() }>close</button>
                <h1>Suggest a VOD</h1>
                <p>
                    <b class="run-name">{run.name}</b>

                    {#if run.category}
                        <span class="run-category">{run.category}</span>
                    {/if}
                </p>

                <form>
                    <label for="suggest-{run_index}-url">URL</label>
                    <input type="text" id="suggest-{run_index}-url" name="url">

                    <label for="suggest-{run_index}-organization">Organization</label>
                    <input type="text" id="suggest-{run_index}-organization" name="organization" value="{ $page.params.org.toLowerCase() }" disabled>

                    <label for="suggest-{run_index}-id" >Run ID</label>
                    <input type="text" id="suggest-{run_index}-id" name="id" value="{ run.id }" disabled>

                    <!-- UI only shown to admins for now (technically api allows users also to replace their own suggestions)-->
                    <!-- in pseudo-code for adding non-admins: run.vods.filter(is_admin || is_same_contributor)  -->
                    {#if ["ADMIN", "MODERATOR"].includes($user.role)}
                        <label for="suggest-{run_index}-replace">Replace existing suggestion</label>
                        <select id="suggest-{run_index}-replace" name="replace">
                            <option value="">None</option>
                            {#each run.vods as vod}
                                <option value="{ vod.url }">{ vod.url }</option>
                            {/each}
                        </select>
                    {/if}

                    <button id="suggest-{run_index}-btn" class="action-btn { submit_status?.toLowerCase() ?? '' }" type="submit" on:click={ submit_suggestion }>
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

            <button class="suggest-btn action-btn" on:click={ () => suggest_dialog().showModal() }>
                <div class="icon" inert>add_circle</div>
                Suggest VOD
            </button>
        {/if}
        
        <p>
            <b class="run-name">{run.name}</b>

            {#if run.category}
                <span class="run-category">{run.category}</span>
            {/if}

            {#if run.src}
                <a class="run-src" href="https://speedrun.com/{run.src}" target="_blank" rel="noopener noreferrer">↗</a>
            {/if}
        </p>



        <div class="run-details">
            <div class="run-time">
                <div class="icon" inert>timer</div>
                { run.runTime }
            </div>
            {#if run.console}
                <div class="run-console">
                    <div class="icon" inert>stadia_controller</div>
                    { run.console }
                </div>
            {/if}
            <div class="run-runners {run.runners.length === 1 ? 'single' : 'multiple'}">
                <div class="icon" inert>{ run.runners.length === 1 ? 'person' : 'group' }</div>
                <span>
                    {#each run.runners as runner, index}
                        {#if index > 0}
                            ,
                        {/if}
                        {#if !!runner.stream}
                            <a href={runner.stream} target="_blank" rel="noopener noreferrer">{runner.name}</a>
                        {:else}
                            {runner.name}
                        {/if}
                    {/each}
                </span>
            </div>

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
