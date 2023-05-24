<script lang="ts">
    import type { Event } from '../gdq';
    import {Formatters} from "$lib/Formatters";

    export let slug: string;

    export let event: Event;

    function toTitleCase(str: string) {
        return str.replace(
            /\w\S*/g,
            function(txt) {
                return txt.charAt(0).toUpperCase() + txt.substring(1).toLowerCase();
            }
        );
    }
</script>

<div>
    {#if event.timeStatus != "FINISHED"}
        <span class="time-status {event.timeStatus.toLowerCase().replace('_', '-')}">
            { toTitleCase(event.timeStatus).replace('_', ' ') }
        </span>
    {/if}
    <a href="/events/{slug}">
        {event.name}
    </a>
    <span class="event-time-info">
        {#if event.timeStatus === "UPCOMING"}
            Begins {Formatters.date_hero(event.startTime)} at {Formatters.time(event.startTime)}
        {:else if event.timeStatus === "FINISHED"}
            Ended {Formatters.date_hero(event.endTime)}
        {/if}
    </span>
</div>




