<script lang="ts">
    import {Formatters} from "$lib/Formatters";
    import type {Bid} from "src/gdq";

    export let bid: Bid;
    export let formatter: Formatters;

    /**
     * Since Tailwind only generates classes as necessary and my code doesn't write out the full
     * names of the classes being used, this comment is here to ensure that all the classes used
     * are generated. So without further ado,
     *
     * text-success text-warning text-error
     * bg-success-content bg-warning-content bg-error-content
     */
    let percent = Math.round((bid.donationTotal / bid.goal) * 100);
    let color = percent >= 100 ? "success" : (bid.state === "OPENED" ? "warning" : "error");
</script>

<p class="block my-auto"><span class="radial-progress text-[.65rem] text-{color} bg-{color}-content" style="--value:{percent}; --size:2.2rem;">{percent}%</span></p>
<div class="bid-body">
    <p>
        <span class="font-semibold">{bid.name}&nbsp;</span>
        <span class="text-base-content/50">{formatter.money(bid.donationTotal)} / {formatter.money(bid.goal)}</span>
    </p>
    <p>{bid.description}</p>
</div>
