<script lang="ts">
    import {Formatters} from "$lib/Formatters";
    import type {Bid} from "vods.speedrun.club-client";

    export let bid: Bid;
    export let formatter: Formatters;

    let percent = Math.round((bid.donationTotal / bid.goal) * 100);
    let color = percent >= 100 ? "var(--bid-full)" : (bid.state === "OPENED" ? "var(--bid-open)" : "var(--bid-closed)");
</script>

<span class="radial-progress" style="--value:{percent}; --size:30px; color:{color}"></span>
<div class="bid-body">
    <b>{bid.name}</b>
    <span class="bid-contributions">{ formatter.money(bid.donationTotal) } / { formatter.money(bid.goal) }</span>
</div>
{#if bid.description}
    <div class="bid-description">{bid.description}</div>
{/if}
