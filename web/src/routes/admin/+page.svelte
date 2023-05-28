<script lang="ts">

    import {onMount} from "svelte";
    import type {VODSuggestion} from "$lib/kgdq";
    import {getSuggestions} from "$lib/kgdq";
    import type {VODSuggestion2} from "$lib/kgdq";

    let suggests: VODSuggestion2[] = [];

    onMount(async () => {

        suggests = await getSuggestions();

    });


</script>

<ul>
    {#each suggests as suggest}
        <li>
            <p>{suggest.vod.type}</p>
            <p>{suggest.vod.url}</p>
        </li>
    {/each}
</ul>