<script lang="ts">
    import {Formatters} from "$lib/Formatters";
    import type {Bid} from "vods.speedrun.club-client";

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
    let color = percent >= 100 ? "var(--bid-full)" : (bid.state === "OPENED" ? "var(--bid-open)" : "var(--bid-closed)");
</script>

<span class="radial-progress" style="--value:{percent}; --size:70px; color:{color}">{percent}%</span>
<div class="bid-body">
    <p>
        <b>{bid.name}&nbsp;</b>
        <span class="bid-contributions">{ formatter.money(bid.donationTotal) } / { formatter.money(bid.goal) }</span>
    </p>
    <p>{bid.description}</p>
</div>
