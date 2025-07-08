<script lang="ts">
    import {Formatters} from "$lib/Formatters";
    import type {Bid} from "vods.speedrun.club-client";

    export let bid: Bid = undefined as unknown as Bid;
    export let formatter: Formatters;

    $: max_bid_total = Math.max(...bid.children.map(c => c.donationTotal));
</script>


<div class="bid-war-stats">

    <div class="bid-war-bars">
        {#if bid.donationTotal > 0}
        {#each bid.children.slice(0,3) as child}
            <div class="bid-war-bar" style="width: {child.donationTotal / max_bid_total * 90}%"></div>
        {/each}
        {/if}
    </div>

    <div class="bid-war-legend">
        {#each bid.children.slice(0,3) as child}
            <div>{child.name}</div>
        {/each}
    </div>
</div>

<div class="bid-body">
    <b>{bid.name}</b>
    <span class="bid-contributions">{formatter.money(bid.donationTotal)}</span>
</div>
{#if bid.description}
    <div class="bid-description">{bid.description}</div>
{/if}
