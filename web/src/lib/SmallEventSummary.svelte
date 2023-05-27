<script lang="ts">
    import type {Event} from 'vods.speedrun.club-client';
    import {Formatters} from "$lib/Formatters";

    export let org: string;

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

<div class="small-event-summary">
    <p><a href="/event/{org}/{event.short}">{event.name}</a></p>
    <span class="event-time-info">
        {#if event.timeStatus === "UPCOMING"}
            Begins {Formatters.date_hero(event.startTime)} at {Formatters.time(event.startTime)}
        {:else if event.timeStatus === "FINISHED"}
            Ended {Formatters.date_hero(event.endTime || event.startTime)}
        {/if}
    </span>
</div>




