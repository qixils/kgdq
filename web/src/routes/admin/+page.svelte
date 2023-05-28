<script lang="ts">

    import {onMount} from "svelte";
    import type {VODSuggestion2} from "$lib/kgdq";
    import {BASE_URL, getSuggestions} from "$lib/kgdq";

    let suggests: VODSuggestion2[] = [];

    onMount(async () => {
        suggests = await getSuggestions();
    });

    async function decide(suggest: VODSuggestion2, accept: boolean) {
        suggests = suggests.filter(s => s.id !== suggest.id);
        let accept_enum = accept ? "APPROVED" : "REJECTED";
        await fetch(`${BASE_URL}/set/suggestion`, { // TODO: do in background? is that a thing? idk
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
    }


</script>

<ul>
    {#if suggests === undefined}
        <li>Loading...</li>
    {:else if suggests.length === 0}
        <li>No suggestions</li>
    {:else}
        {#each suggests as suggest}
            <li>
                <p>{suggest.vod.type}</p>
                <p>{suggest.vod.url}</p>
                <button on:click={() => decide(suggest, true)}>Accept</button>
                <button on:click={() => decide(suggest, false)}>Reject</button>
            </li>
        {/each}
    {/if}
</ul>