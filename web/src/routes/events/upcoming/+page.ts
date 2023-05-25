import {SvcClient} from "vods.speedrun.club-client";

const svc = new SvcClient();
declare const epoch = "1970-01-01T00:00:00Z";

export async function load() {
    return { events: // wrapped as object since I guess svelte doesn't serialize arrays as arrays
            (await svc.getAllEventsFlat())
                .filter(event => event.timeStatus === "UPCOMING" || event.timeStatus === "IN_PROGRESS")
                // From order by ascending start time, top of list might end soonest
                .sort((a, b) => (a.startTime || epoch).localeCompare(b.startTime || epoch) )
    };
}