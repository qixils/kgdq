<script lang="ts">
    import {Formatters} from "$lib/Formatters";
    import type {Bid} from "vods.speedrun.club-client";

    export let bid: Bid;
    export let formatter: Formatters;
    let goal = bid.goal || 1;

    let percent = Math.round((bid.donationTotal / (bid.goal || goal)) * 100);
    let color = `var(--bid-${percent >= 100 ? "full" : (bid.state === "OPENED" ? "open" : "closed")})`;

</script>

<span class="radial-progress" style="--value:{percent}; --size:30px; color:{color}"></span>
<div class="bid-body">
    <b>{bid.name}</b>
    <span class="bid-contributions">{ formatter.money(bid.donationTotal) } / { formatter.money(goal) }</span>
</div>
{#if bid.description}
    <div class="bid-description">{bid.description}</div>
{/if}
