let API_DOMAIN = 'vods.speedrun.club';
export let BASE_URL= 'https://' + API_DOMAIN + '/api/v2';

let ORG_IDS = ['gdq', 'esa', 'hek', 'rpglb'];

import type {Event, Run} from "src/gdq";

export async function fetchEvents(org: string): Promise<Event[]> {
    let res = await fetch(`${BASE_URL}/marathons/${org}/events`);
    return ((await res.json()) as Event[])
        .sort((a, b) => -(a.startTime).localeCompare(b.startTime!) );
}

export async function fetchAllEvents() {
    let events: {org: string, event: Event}[] = [];
    for (let org of ORG_IDS) {
        for (let event of await fetchEvents(org)) {
            console.log(event)
            events.push({org: org, event: event});
        }
    }
    return events;
}

export function fake_status(run: Run, index: number) {
    let current_status_index  = 61;
    if (index == current_status_index) {
        return "IN_PROGRESS";
    } else if (index < current_status_index) {
        return "FINISHED";
    } else {
        return "UPCOMING";
    }
}