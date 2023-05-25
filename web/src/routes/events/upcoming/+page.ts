import {SvcClient} from "vods.speedrun.club-client";
import type {PageLoad} from './$types';
import {BASE_URL} from "$lib/kgdq";

declare const epoch = "1970-01-01T00:00:00Z";

export const load = (async ({ fetch }) => {
    return { events: // wrapped as object since I guess svelte doesn't serialize arrays as arrays
            (await new SvcClient(BASE_URL, fetch).getAllEventsFlat())
                .filter(event => event.timeStatus === "UPCOMING" || event.timeStatus === "IN_PROGRESS")
                // From order by ascending start time, top of list might end soonest
                .sort((a, b) => (a.startTime || epoch).localeCompare(b.startTime || epoch) )
    };
}) satisfies PageLoad;