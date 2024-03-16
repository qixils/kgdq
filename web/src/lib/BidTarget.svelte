<script lang="ts">
    import {Formatters} from "$lib/Formatters";
    import type {Bid} from "vods.speedrun.club-client";

    export let bid: Bid = undefined as Bid;
    export let formatter: Formatters;

    let percent = Math.round((bid.donationTotal / bid.goal) * 100);
    let color = "--bid-" + (percent >= 100 ? "full" : (bid.state === "OPENED" ? "open" : "closed"));
    let textColor = `var(${color}-text)`;
    color = `var(${color})`;

</script>

<span class="radial-progress" style="--value:{percent}; --size:70px; --edge-color:{color}; color:{textColor}">{percent}%</span>
<div class="bid-body">
    <p>
        <b>{bid.name}&nbsp;</b>
        <span class="bid-contributions">{ formatter.money(bid.donationTotal) } / { formatter.money(bid.goal) }</span>
    </p>
    <p>{bid.description}</p>
</div>
