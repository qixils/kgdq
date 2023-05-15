<script lang="ts">
    import {Formatters} from "$lib/Formatters";
    import VODs from "$lib/VODs.svelte";
    import type {Run} from "src/gdq";
    import BidTarget from "$lib/BidTarget.svelte";
    import BidWar from "$lib/BidWar.svelte";

    export let runs: Run[];
    export let run_index: number;
    export let formatter: Formatters;

    let run = runs[run_index];
    let step_color = run.timeStatus === "UPCOMING" ? "" : (run.timeStatus === "IN_PROGRESS" ? "step-primary" : "step-secondary");
</script>

<li data-content="" class="step {step_color} text-base-content" style="z-index: -{run_index}">
    <div class="flex w-full align-center gap-2">
        <p class="text-center block my-auto basis-8 font-light md:basis-10 md:font-normal flex-shrink-0" style="position: relative; top:-.175rem;">
            {Formatters.time(run.startTime)}
        </p>
        <div class="text-left p-2 bg-base-300 block flex-grow">
            {#if run_index === 0 || new Date(run.startTime).getDay() !== new Date(runs[run_index - 1].startTime).getDay()}
                {#if run_index > 0}
                    <hr class="border-neutral/50" style="position: relative; top:-.6rem;">
                {/if}
                <p class="text-base md:text-lg bg-primary text-primary-content p-2 pl-3 rounded-t font-semibold">{Formatters.date_header(run.startTime)}</p>
                <hr class="border-primary/70 border-2 mb-3">
            {/if}
            <p>
                {#if run.vods.length > 0}
                    <VODs {run} />
                {/if}
                <span style="position: relative; top:-.1em;">
                    <b>{run.name}</b
                    >{#if run.src !== null && run.src !== undefined}
                    <sup>
                        <a href="https://speedrun.com/{run.src}" target="_blank" rel="noopener noreferrer">[src]</a>
                    </sup>
                    {/if}
                    {#if run.category !== ""}
                        <span class="text-base-content/80">&nbsp; {run.category}</span>
                    {/if}
                </span>
            </p>
            <p>
                <span class="material-symbols-rounded text-sm">timer</span
                ><span class="post-icon">&nbsp;{run.runTime}&nbsp;&nbsp;&nbsp;</span
            >{#if run.console !== ""
            }<span class="material-symbols-rounded text-sm">stadia_controller</span
            ><span class="post-icon">&nbsp;{run.console}</span>{/if}
            </p>
            <p class="runners">
                <span class="material-symbols-rounded text-sm">{run.runners.length === 1 ? 'person' : 'group'}</span
                ><span class="post-icon">
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
            </p>
            {#each run.bids as bid}
                <div class="flex flex-center gap-2 py-1">
                    {#if bid.isTarget}
                        <BidTarget {bid} {formatter} />
                    {:else}
                        <BidWar {bid} {formatter} />
                    {/if}
                </div>
            {/each}
        </div>
    </div>
</li>

<style>
    .post-icon {
        position: relative;
        top: -0.15rem;
    }
</style>
