<script lang="ts">

    import {onMount} from "svelte";
    import type {VODSuggestion} from "$lib/kgdq";
    import {BASE_URL, getSuggestions} from "$lib/kgdq";
    import type {MarathonEvent, Run} from "vods.speedrun.club-client";
    import {svc} from "vods.speedrun.club-client";
    import LoadingSkeleton from "$lib/LoadingSkeleton.svelte";

    let suggests: VODSuggestion[];

    let suggests_with_run_event: { run: Run, suggest: VODSuggestion, event: MarathonEvent }[] = [];
    let suggests_populated = false;
    let events_cache: {[org: string]: MarathonEvent[]} = {};
    let runs_cache: {[slug: string]: Run[]} = {};

    onMount(async () => {
        suggests = await getSuggestions();
        suggests_with_run_event = [];

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
                let run = event_runs.find(r => r.id !== null && r.id === suggest.runId);
                if (run) {
                    suggests_with_run_event = [...suggests_with_run_event ?? [], {run, suggest, event}];
                    break;
                }
            }
        }

        suggests_populated = true;
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

    async function resetIgdb() {
        const res = await fetch(`${BASE_URL}/resetigdb`, { method: "PUT", credentials: "include", headers: { "Content-Type": "application/json" } })
        alert(`${res.status} ${res.statusText}`)
    }
</script>

<p>SECRET ADMIN VOD APPROVAL PAGE</p>

<button on:click={() => resetIgdb()}>Clear IGDB empty cached results</button>

{#if !suggests_populated}
    <LoadingSkeleton />
{:else if suggests_with_run_event.length === 0}
    <p>No suggestions</p>
{:else}
    <ul>
    {#each suggests_with_run_event as {run, suggest, event}}
        <li>
            <p>for <b>{run.name}</b> / {run.category} during {event.name}</p>
            <p>{suggest.vod.type} <a href={suggest.vod.url} target="_blank">{suggest.vod.url}</a></p>
            <p>User: <code>{suggest.vod.contributorId}</code>, ID: <code>{suggest.id}</code>, Run: <code>{suggest.runId}</code></p>
            <p>{suggest.organization}</p>
            <button on:click={() => decide(suggest, true)}>Accept</button>
            <button on:click={() => decide(suggest, false)}>Reject</button>
        </li>
    {/each}
    </ul>
{/if}
