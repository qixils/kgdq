let API_DOMAIN = 'vods.speedrun.club';
let BASE_URL= 'https://' + API_DOMAIN + '/api/v2';

let ORG_IDS = ['gdq', 'esa', 'hek', 'rpglb'];

import type { Event } from "../gdq";

export async function fetchEvents(org: string): Promise<Event[]> {
    let res = await fetch(`${BASE_URL}/marathons/${org}/events`);
    return await res.json();
}

export async function fetchAllEvents() {
    let events: {slug: string, event: Event}[] = [];
    for (let org of ORG_IDS) {
        for (let event of await fetchEvents(org)) {
            events.push({slug: org, event: event});
        }
    }
    return events;
}