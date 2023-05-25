import {fetchAllEvents} from "$lib/kgdq";

export async function load() {
    return { events: // wrapped as object since I guess svelte doesn't serialize arrays as arrays
            (await fetchAllEvents())
                .filter(({event}) => event.timeStatus === "FINISHED")
                // From most recent to least recent
                // endTime is assumed to be not null because the events have finished
                .sort(({event: a}, {event: b}) => -(a.endTime || a.startTime).localeCompare(b.endTime || b.startTime))
    };
}