<script lang="ts">
    import type {Event, Run} from 'src/gdq';
    import {page} from "$app/stores";
    // TODO: handle the case where the event is not found
    let event_promise: Promise<Event> = fetch(`https://vods.speedrun.club/api/v1/${$page.params.org}/events?id=${$page.params.slug}`)
        .then(r => r.json()).then(r => r[0]);
    let runs_promise: Promise<Run[]> = fetch(`https://vods.speedrun.club/api/v1/${$page.params.org}/runs?event=${$page.params.slug}`)
        .then(r => r.json())

    let money_format = new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' });
    function money(n: number) {
        let res = money_format.format(n);
        if (res.endsWith('.00')) {
            res = res.slice(0, -3);
        }
        return res;
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
                <div class="max-w-md">
                    <h1 class="text-5xl font-bold">{event.name}</h1>
                    <!-- TODO: display end date after runs load -->
                    <p class="pt-6">{event.short} began on {event.datetime} and raised {money(event.amount)} for {event.charityName}.</p>
                </div>
            </div>
        </div>

        {#await runs_promise}
            <div class="text-center my-3"><button class="btn loading">loading</button></div>
        {:then runs}
            <ul class="steps steps-vertical block w-full max-w-screen-lg mx-auto">
                {#each runs as run}
                    <!-- TODO: line separator/divider? -->
                    <!-- TODO: only use step-secondary if run has passed; if up next, use step-primary; else, use nothing -->
                    <li data-content="" class="step step-secondary">
                        <div class="text-left p-2 bg-neutral text-neutral-content w-full block">
                            <!-- TODO
                            {#if run.twitchVODs.length > 0 || run.youtubeVODs.length > 0}
                                <div class="dropdown dropdown-hover">
                                    <label tabindex="0" class="btn">A</label>
                                    <ul tabindex="0" class="dropdown-content menu p-2 shadow bg-base-100 rounded-box w-52">
                                        <li><a>Item 1</a></li>
                                        <li><a>Item 2</a></li>
                                    </ul>
                                </div>
                            {/if}
                            -->
                            <p>
                                <b>{run.name}</b>
                                &nbsp;
                                <span class="text-neutral-content/80">{run.category}</span>
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
                                        {#if runner.url !== null}
                                            <a href={runner.url} target="_blank" rel="noopener noreferrer">{runner.name}</a>
                                        {:else}
                                            {runner.name}
                                        {/if}
                                    {/each}
                                </span>
                            </p>
                            {#each run.bids as bid}
                                <div class="flex flex-start flex-center gap-2 py-1">
                                    {#if bid.isTarget}
                                        <!--
                                        Since Tailwind only generates classes as necessary and my code doesn't write out
                                        the full names of the classes being used, this comment is here to ensure that
                                        all of the classes used are generated. So without further ado,
                                        text-success text-warning text-error
                                        bg-success-content bg-warning-content bg-error-content
                                        -->
                                        {@const percent = Math.round((bid.donationTotal / bid.goal) * 100)}
                                        {@const color = percent >= 100 ? "success" : (bid.state === "OPENED" ? "warning" : "error")}
                                        <p class="block my-auto"><span class="radial-progress text-[.65rem] text-{color} bg-{color}-content" style="--value:{percent}; --size:2.2rem;">{percent}%</span></p>
                                        <div class="bid-body">
                                            <p>
                                                <span class="font-semibold">{bid.name}&nbsp;</span>
                                                <span class="text-neutral-content/50">{money(bid.donationTotal)} / {money(bid.goal)}</span>
                                            </p>
                                            <p>{bid.description}</p>
                                        </div>
                                    {:else}
                                        <p class="block my-auto"><span class="radial-progress text-[.65rem] text-primary bg-primary/20" style="--value:100; --size:2.2rem;"></span></p>
                                        <div class="bid-body">
                                            <p>
                                                <span class="font-semibold">{bid.name}&nbsp;</span>
                                                <span class="text-neutral-content/50">{money(bid.donationTotal)}</span>
                                            </p>
                                            <p>{bid.description}</p>
                                            <p class="text-neutral-content/80">
                                                <!-- TODO: these are sorted by descending donationTotal... right? -->
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

    .post-icon {
        position: relative;
        top: -0.15rem;
    }

    .bid-body {
        @apply block my-auto flex-grow text-sm;
    }
</style>