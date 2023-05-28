import {SvcClient} from "vods.speedrun.club-client";
import type {PageLoad} from './$types';
import {BASE_URL, compareEventEndTime} from "$lib/kgdq";

export const load = (async ({ fetch }) => {
    return { events: // wrapped as object since I guess svelte doesn't serialize arrays as arrays
            (await new SvcClient(BASE_URL, fetch).getAllEventsFlat())
                .filter(event => event.timeStatus === "FINISHED")
                // From most recent to least recent
                .sort(compareEventEndTime).reverse()
                // Only 20 most recent
                .slice(0, 20)
    };
}) satisfies PageLoad;