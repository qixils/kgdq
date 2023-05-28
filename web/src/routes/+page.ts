import {SvcClient} from "vods.speedrun.club-client";
import type {PageLoad} from './$types';
import {BASE_URL, compareEventStartTime} from "$lib/kgdq";

export const load = (async ({ fetch }) => {
    return { events:
            (await new SvcClient(BASE_URL, fetch).getAllEventsFlat())
                .filter(event => event.timeStatus === "IN_PROGRESS")
                // From order by ascending start time, top of list might end soonest
                .sort(compareEventStartTime)
    };
}) satisfies PageLoad;