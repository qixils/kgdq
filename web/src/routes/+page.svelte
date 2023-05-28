<script lang="ts">
	import PageHeadTags from '$lib/PageHeadTags.svelte';
	import type {OrganizedEvent} from "vods.speedrun.club-client";
	import EventSummary from "$lib/EventSummary.svelte";

	export let data: {events: OrganizedEvent[]};
</script>

<svelte:head>
	<PageHeadTags
		description="Find VODs of speedruns from charity marathon streams." />
</svelte:head>

<div style="text-align: center; max-width: 600px; margin: 0 auto">
	<p class="hero-text">A site to find VODs of speedruns that are available online from charity marathon streams.</p>
	<p>"VOD" is short for <em>Video On Demand</em>, it means a record of the livestream that can be viewed any time after the event.</p>
</div>

{#if data.events.length > 0}
	<h1>Current Events</h1>
	<ul class="events-list">
		{#each data.events as event}
			<EventSummary  org={event.organization} event={event} />
		{/each}
	</ul>
{/if}