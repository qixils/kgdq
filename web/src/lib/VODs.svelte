<script lang="ts">
    import type {Run} from "src/gdq";
    import VODEntry from "$lib/VODEntry.svelte";

    export let run: Run;
    let vodCount = new Map();
</script>

<div class="dropdown">
    <label tabindex="0"><span class="hover:bg-info-content rounded-box material-symbols-rounded text-sm text-info">play_circle</span></label>
    <ul tabindex="0" class="dropdown-content menu p-2 shadow bg-base-100 rounded-box w-56">
        {#each run.vods as vod}
            <VODEntry vod={vod} index={vodCount.set(vod.type, (vodCount.get(vod.type) ?? -1) + 1).get(vod.type)} />
        {/each}
    </ul>
</div>
