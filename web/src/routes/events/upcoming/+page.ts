import {SvcClient} from "vods.speedrun.club-client";
import type {PageLoad} from './$types';
import {BASE_URL, compareEventStartTime} from "$lib/kgdq";

export const load = (async ({ fetch }) => {
    try {
        return { events: // wrapped as object since I guess svelte doesn't serialize arrays as arrays
                (await new SvcClient(BASE_URL, fetch).getAllEvents())
                    .filter(event => event.timeStatus === "UPCOMING" )
                    // From order by ascending start time, top of list might end soonest
                    .sort(compareEventStartTime)
        };
    } catch (e) {
        return { events: e };
    }
}) satisfies PageLoad;