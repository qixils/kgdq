<script lang="ts">
    import type {Event, Run} from 'src/gdq';
    import {page} from "$app/stores";
    // TODO: handle the case where the event is not found
    let event_promise: Promise<Event> = fetch(`https://vods.speedrun.club/api/v2/marathons/${$page.params.org}/events?id=${$page.params.slug}`)
        .then(r => r.json()).then(r => r[0]);
    let runs_promise: Promise<Run[]> = fetch(`https://vods.speedrun.club/api/v2/marathons/${$page.params.org}/runs?event=${$page.params.slug}`)
        .then(r => r.json())

    let money_format = new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' });
    function money(n: number) {
        let res = money_format.format(n);
        if (res.endsWith('.00')) {
            res = res.slice(0, -3);
        }
        return res;
    }

    let date_header_format = new Intl.DateTimeFormat(undefined, { dateStyle: 'full' });
    function date_header(dt: string) {
        return date_header_format.format(new Date(dt));
    }
    let date_hero_format = new Intl.DateTimeFormat(undefined, { dateStyle: 'long' });
    function date_hero(dt: string) {
        return date_hero_format.format(new Date(dt));
    }
    let time_format = new Intl.DateTimeFormat(undefined, { timeStyle: 'short' });
    function time(dt: string) {
        return time_format.format(new Date(dt));
    }
</script>

<svelte:head>
    <title></title>
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
                        {#await runs_promise}
                            began on {date_hero(event.datetime)}
                        {:then runs}
                            ran from {date_hero(event.datetime)} to {date_hero(runs[runs.length - 1].endTime)}
                        {/await}
                        and raised {money(event.amount)} for {event.charityName}.
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
                    {@const step_color = run.scheduleStatus === "UPCOMING" ? "" : (run.scheduleStatus === "IN_PROGRESS" ? "step-primary" : "step-secondary")}
                    <li data-content="" class="step {step_color} text-base-content" style="z-index: -{run_index}">
                        <div class="flex w-full align-center gap-2">
                            <p class="text-center block my-auto basis-8 font-light md:basis-10 md:font-normal flex-shrink-0" style="position: relative; top:-.175rem;">
                                {time(run.startTime)}
                            </p>
                            <div class="text-left p-2 bg-base-300 block flex-grow">
                                {#if run_index === 0 || new Date(run.startTime).getDay() !== new Date(runs[run_index - 1].startTime).getDay()}
                                    {#if run_index > 0}
                                        <hr class="border-neutral/50" style="position: relative; top:-.6rem;">
                                    {/if}
                                    <p class="text-base md:text-lg bg-primary text-primary-content p-2 pl-3 rounded-t font-semibold">{date_header(run.startTime)}</p>
                                    <hr class="border-primary/70 border-2 mb-3">
                                {/if}
                                <p>
                                    {#if run.vods.length > 0}
                                        <!-- TODO: move to a component -->
                                        {@const vodCount = new Map()}
                                        <div class="dropdown">
                                            <label tabindex="0"><span class="hover:bg-info-content rounded-box material-symbols-rounded text-sm text-info">play_circle</span></label>
                                            <ul tabindex="0" class="dropdown-content menu p-2 shadow bg-base-100 rounded-box w-56">
                                                {#each run.vods as vod}
                                                    <!-- TODO: move to a component -->
                                                    {@const index = vodCount.set(vod.type, (vodCount.get(vod.type) ?? -1) + 1).get(vod.type)}
                                                    <li><a href={vod.url} target="_blank" rel="noopener noreferrer">
                                                        Watch
                                                        {#if vod.type === "YOUTUBE"}
                                                            on YouTube
                                                        {:else if vod.type === "TWITCH"}
                                                            on Twitch
                                                        {/if}
                                                        {#if index > 0}(Part {index + 1}){/if}
                                                    </a></li>
                                                {/each}
                                            </ul>
                                        </div>
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
                                            <!--
                                            Since Tailwind only generates classes as necessary and my code doesn't write out
                                            the full names of the classes being used, this comment is here to ensure that
                                            all of the classes used are generated. So without further ado,
                                            text-success text-warning text-error
                                            bg-success-content bg-warning-content bg-error-content
                                            -->
                                            <!-- TODO: move to a component -->
                                            {@const percent = Math.round((bid.donationTotal / bid.goal) * 100)}
                                            {@const color = percent >= 100 ? "success" : (bid.state === "OPENED" ? "warning" : "error")}
                                            <p class="block my-auto"><span class="radial-progress text-[.65rem] text-{color} bg-{color}-content" style="--value:{percent}; --size:2.2rem;">{percent}%</span></p>
                                            <div class="bid-body">
                                                <p>
                                                    <span class="font-semibold">{bid.name}&nbsp;</span>
                                                    <span class="text-base-content/50">{money(bid.donationTotal)} / {money(bid.goal)}</span>
                                                </p>
                                                <p>{bid.description}</p>
                                            </div>
                                        {:else}
                                            <p class="block my-auto"><span class="radial-progress text-[.65rem] text-primary bg-primary/30" style="--value:100; --size:2.2rem;"></span></p>
                                            <div class="bid-body">
                                                <p>
                                                    <span class="font-semibold">{bid.name}&nbsp;</span>
                                                    <span class="text-base-content/50">{money(bid.donationTotal)}</span>
                                                </p>
                                                <p>{bid.description}</p>
                                                <p class="text-base-content/80">
                                                    <span class="font-semibold">Top Options:</span>
                                                    {#each bid.children.slice(0,3) as child, index}
                                                        {#if index > 0}
                                                            ,
                                                        {/if}
                                                        <span class:underline={index===0}>{child.name}</span>
                                                    {/each}
                                                </p>
                                            </div>
                                        {/if}
                                    </div>
                                {/each}
                            </div>
                        </div>
                    </li>
                {/each}
            </ul>
        {/await}
    {/await}
</section>

<style>
    a {
        @apply text-accent;
    }

    a:hover {
        @apply text-accent-focus;
    }

    .post-icon {
        position: relative;
        top: -0.15rem;
    }

    .bid-body {
        @apply block my-auto flex-grow text-xs md:text-sm;
    }
</style>