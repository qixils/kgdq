<script lang="ts">
    import type {VOD} from "vods.speedrun.club-client";

    export let vod: VOD;
    export let index: number;
</script>

<li><a href={vod.url} target="_blank" rel="noopener noreferrer">
    Watch
    {#if vod.type === "YOUTUBE"}
        on YouTube
    {:else if vod.type === "TWITCH"}
        on Twitch
    {/if}
    {#if index > 0}(Part {index + 1}){/if}
</a></li>