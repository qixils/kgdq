import {fetchAllEvents} from "$lib/kgdq";

export async function load() {
    return { events: // wrapped as object since I guess svelte doesn't serialize arrays as arrays
            (await fetchAllEvents())
                .filter(({event}) => event.timeStatus === "UPCOMING" || event.timeStatus === "IN_PROGRESS")
                // From order by ascneding start time, top of list might end soonest
                .sort(({event: a}, {event: b}) => a.startTime.localeCompare(b.startTime) )
    };
}