<script lang="ts">
    import type {VOD} from "vods.speedrun.club-client";

    export let vod: VOD = undefined as VOD;
    export let index: number;

    let platform_nice = vod.type === "YOUTUBE" ? "YouTube" : "Twitch";
    let platform_css = platform_nice.toLowerCase();

</script>

<li class="vod { platform_css }">
    <a href={vod.url} target="_blank" rel="noopener noreferrer">
    { platform_nice } {#if index > 0}(Part {index + 1}){/if}
    </a>
</li>