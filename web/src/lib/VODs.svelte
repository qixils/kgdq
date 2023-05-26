<script lang="ts">
    import type {Run} from "vods.speedrun.club-client";
    import VODEntry from "$lib/VODEntry.svelte";

    export let run: Run;
    let vodCount = new Map();
</script>

<ul class="vods-list" tabindex="0">
    {#each run.vods as vod}
        <VODEntry vod={vod} index={vodCount.set(vod.type, (vodCount.get(vod.type) ?? -1) + 1).get(vod.type)} />
    {/each}
</ul>
