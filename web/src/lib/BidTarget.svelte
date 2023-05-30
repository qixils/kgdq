<script lang="ts">
    import {Formatters} from "$lib/Formatters";
    import type {Bid} from "vods.speedrun.club-client";

    export let bid: Bid = undefined as Bid;
    export let formatter: Formatters;

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
