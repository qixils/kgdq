<script lang="ts">
	import PageHeadTags from '$lib/PageHeadTags.svelte';
	import type {MarathonEvent} from "vods.speedrun.club-client";
	import {SvcClient} from "vods.speedrun.club-client";
	import EventSummary from "$lib/EventSummary.svelte";
	import {onMount} from "svelte";
	import {BASE_URL, compareEventStartTime} from "$lib/kgdq";
	import LoadingSkeleton from "$lib/LoadingSkeleton.svelte";
	import ErrorReport from "$lib/ErrorReport.svelte";

	let events: MarathonEvent[];
	let events_error: Error | null = null;

	onMount(async () => {
		try {
			events = (await new SvcClient(BASE_URL, fetch).getAllEvents())
					.filter(event => event.timeStatus === "IN_PROGRESS")
					// From order by ascending start time, top of list might end soonest
					.sort(compareEventStartTime);
		} catch (e) {
			events_error = e;
		}
	});
</script>

<svelte:head>
	<PageHeadTags
		description="Find VODs of speedruns from charity marathon streams."
	    url="https://vods.speedrun.club" />
</svelte:head>

<div style="text-align: center; max-width: 600px; margin: 0 auto">
	<p class="hero-text">A site to find VODs of speedruns that are available online from charity marathon streams.</p>
	<p>"VOD", short for <em>Video On Demand</em>, is a recording of the livestream that can be viewed any time after the event.</p>
</div>

<h1>Current Events</h1>
{#if events_error !== null}
	<ErrorReport message="Failed to load events: {events_error.message}" />
{:else if events === undefined}
	<LoadingSkeleton size="40em" />
{:else if events.length > 0}
	<ul class="events-list">
		{#each events as event}
			<EventSummary  org={event.organization} event={event} />
		{/each}
	</ul>
{:else}
	<p>No events are currently in progress. You should check out some <a href="/events/recent">recent events</a> instead!</p>
{/if}