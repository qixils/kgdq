import {SvcClient} from "vods.speedrun.club-client";

const svc = new SvcClient();
declare const epoch = "1970-01-01T00:00:00Z";

export async function load() {
    return { events: // wrapped as object since I guess svelte doesn't serialize arrays as arrays
            (await svc.getAllEventsFlat())
                .filter(event => event.timeStatus === "FINISHED")
                // From most recent to least recent
                .sort((a, b) => -(a.endTime || a.startTime || epoch).localeCompare(b.endTime || b.startTime || epoch))
    };
}