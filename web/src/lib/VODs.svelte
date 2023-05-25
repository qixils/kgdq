<script lang="ts">
    import type {Run} from "vods.speedrun.club-client";
    import VODEntry from "$lib/VODEntry.svelte";

    export let run: Run;
    let vodCount = new Map();
</script>

<details class="dropdown">
    <summary>VODs</summary>
    <label tabindex="0"><span class="material-symbols-rounded">play_circle</span></label>
    <ul tabindex="0">
        {#each run.vods as vod}
            <VODEntry vod={vod} index={vodCount.set(vod.type, (vodCount.get(vod.type) ?? -1) + 1).get(vod.type)} />
        {/each}
    </ul>
</details>
