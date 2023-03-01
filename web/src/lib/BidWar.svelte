<script lang="ts">
    import {Formatters} from "$lib/Formatters";
    import type {Bid} from "src/gdq";

    export let bid: Bid;
    export let formatter: Formatters;
</script>

<p class="block my-auto"><span class="radial-progress text-[.65rem] text-primary bg-primary/30" style="--value:100; --size:2.2rem;"></span></p>
<div class="bid-body">
    <p>
        <span class="font-semibold">{bid.name}&nbsp;</span>
        <span class="text-base-content/50">{formatter.money(bid.donationTotal)}</span>
    </p>
    <p>{bid.description}</p>
    <p class="text-base-content/80">
        <span class="font-semibold">Top Options:</span>
        {#each bid.children.slice(0,3) as child, index}
            {#if index > 0}
                ,
            {/if}
            <span class:underline={index===0}>{child.name}</span>
        {/each}
    </p>
</div>
