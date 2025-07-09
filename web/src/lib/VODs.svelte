<script lang="ts">
    import type {Run} from "vods.speedrun.club-client";
    import VODEntry from "$lib/VODEntry.svelte";

    export let run: Run;
</script>

<ul class="vods-list">
    {#each run.vods as vod, i}
        {@const index = run.vods.slice(0, i).reduce((part, v) => vod.type == v.type ? part + 1 : part, 0)}
        <VODEntry {vod} {index} />
    {/each}
</ul>
