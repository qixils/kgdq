<script lang="ts">

    import {onMount} from "svelte";
    import type {VODSuggestion} from "$lib/kgdq";
    import {BASE_URL, getSuggestions} from "$lib/kgdq";
    import type {Event, Run} from "vods.speedrun.club-client";
    import {svc} from "vods.speedrun.club-client";
    import PageHeadTags from "$lib/PageHeadTags.svelte";
    import LoadingSkeleton from "$lib/LoadingSkeleton.svelte";

    let suggests: VODSuggestion[];

    let suggests_with_run_event: { run: Run, suggest: VODSuggestion, event: Event }[];
    let events_cache: {[org: string]: Event[]} = {};
    let runs_cache: {[slug: string]: Run[]} = {};

    onMount(async () => {
        suggests = await getSuggestions();

        for (let suggest of suggests) {
            console.log("Suggest", suggest);

            if (!events_cache[suggest.organization]) {
                console.log("Fetching events for", suggest.organization);
                events_cache[suggest.organization] = (await svc.getEvents(suggest.organization)).reverse();
            }

            let events_for_org = events_cache[suggest.organization];

            for (let event of events_for_org) {
                if (!runs_cache[event.short]) {
                    console.log("Fetching runs for", event.short);
                    runs_cache[event.short] = await svc.getRuns(suggest.organization, event.short);
                }
                let event_runs = runs_cache[event.short];
                let run = event_runs.find(r => r.horaroId === suggest.horaroId);
                if (run) {
                    suggests_with_run_event = [...suggests_with_run_event ?? [], {run, suggest, event}];
                    break;
                }
            }
        }
    });

    async function decide(suggest: VODSuggestion, accept: boolean) {
        suggests = suggests.filter(s => s.id !== suggest.id);
        let accept_enum = accept ? "APPROVED" : "REJECTED";
        let res = await fetch(`${BASE_URL}/set/suggestion`, { // TODO: do in background? is that a thing? idk
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                id: suggest.id,
                action: accept_enum
            }),
            credentials: "include"
        });
        if (!res.ok) {
            alert(`Failed to ${accept_enum.toLowerCase()} suggestion ${suggest.id}`);
            return;
        }
        alert(`Suggestion ${suggest.id} ${accept_enum.toLowerCase()}`);

        suggests_with_run_event = suggests_with_run_event.filter(s => s.suggest.id !== suggest.id);

    }


</script>

<svelte:head>
    <PageHeadTags
            no_index={true}
            title="Admin >:3"
            description="Modify event data and approve VOD suggestions." />
</svelte:head>

<p>SECRET ADMIN VOD APPROVAL PAGE</p>

{#if suggests_with_run_event === undefined}
    <LoadingSkeleton />
{:else if suggests_with_run_event.length === 0}
    <p>No suggestions</p>
{:else}
    <ul>
    {#each suggests_with_run_event as {run, suggest, event}}
        <li>
            <p>for <b>{run.name}</b> / {run.category} during {event.public}</p>
            <p>{suggest.vod.type} {suggest.vod.url}</p>
            <p>ID: <code>{suggest.id}</code>, GDQ: <code>{suggest.gdqId}</code>, Horaro: <code>{suggest.horaroId}</code></p>
            <p>{suggest.organization}</p>
            <button on:click={() => decide(suggest, true)}>Accept</button>
            <button on:click={() => decide(suggest, false)}>Reject</button>
        </li>
    {/each}
    </ul>
{/if}
