<script lang="ts">

    import {onMount} from "svelte";
    import type {VODSuggestion} from "$lib/kgdq";
    import {getSuggestions} from "$lib/kgdq";
    import type {VODSuggestion2} from "$lib/kgdq";

    let suggests: VODSuggestion2[] = [];

    onMount(async () => {

        suggests = await getSuggestions();

    });

    function decide(suggest: VODSuggestion2, accept: boolean) {
        let accept_enum = accept ? "APPROVED" : "REJECTED";
        console.log(`${suggest.vod.url}: ${accept_enum}`);
    }


</script>

<ul>
    {#each suggests as suggest}
        <li>
            <p>{suggest.vod.type}</p>
            <p>{suggest.vod.url}</p>
            <button on:click={() => decide(suggest, true)}>Accept</button>
            <button on:click={() => decide(suggest, false)}>Reject</button>
        </li>
    {/each}
</ul>