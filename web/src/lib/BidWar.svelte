<script lang="ts">
    import {Formatters} from "$lib/Formatters";
    import type {Bid} from "src/gdq";

    export let bid: Bid;
    export let formatter: Formatters;
</script>

<span class="radial-progress" style="--value:100; --size:70px;">BID WAR</span>
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
