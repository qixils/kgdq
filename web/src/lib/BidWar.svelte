<script lang="ts">
    import {Formatters} from "$lib/Formatters";
    import type {Bid} from "vods.speedrun.club-client";

    export let bid: Bid = undefined as Bid;
    export let formatter: Formatters;

    let max_bid_total = Math.max(...bid.children.map(c => c.donationTotal));
</script>

<div class="bid-war-bars">
    {#each bid.children.slice(0,3) as child}
        <div class="bid-war-bar" style="width: {child.donationTotal / max_bid_total * 90}%"></div>
    {/each}
</div>
<div class="bid-body">
    <p>
        <b>{bid.name}&nbsp;</b>
        <span class="bid-contributions">{formatter.money(bid.donationTotal)}</span>
    </p>
    <p>{bid.description}</p>
    <p>
        <b>Top Options:</b>
        {#each bid.children.slice(0,3) as child, index}
            {#if index > 0}
                ,
            {/if}
            <span class:underline={index===0}>{child.name}</span>
        {/each}
    </p>
</div>
