import type {Event} from "vods.speedrun.club-client";

let API_DOMAIN = 'vods.speedrun.club';
export let BASE_URL= 'https://' + API_DOMAIN + '/api/v2';

export const EPOCH = "1970-01-01T00:00:00Z";

export function compareEventStartTime(a: Event, b: Event) {
    return (a.startTime || EPOCH).localeCompare(b.startTime || EPOCH);
}

export function compareEventEndTime(a: Event, b: Event) {
    return (a.endTime || a.startTime || EPOCH).localeCompare(b.endTime || b.startTime || EPOCH);
}

export declare class VodLink {
    type: "YOUTUBE" | "TWITCH" | "OTHER";
    videoId: string;
    timestamp: number | null;
    url: string;
    contributorId: string;
}
export declare class VODSuggestion {
    vod: VodLink;
    organization: string;
    gdqId: number | null;
    horaroId: string | null;
    id: string;
}

export declare class User {
    id: string;
    name: string;
}

async function get<T>(url: string): Promise<T> {
    let res = await fetch(url, { credentials: 'include'});
    if (res.status >= 400) {
        throw new Error(`Request failed with status code ${res.status}`);
    }
    return await res.json();
}

export async function getSuggestions(): Promise<VODSuggestion[]> {
    return await get<VODSuggestion[]>(`${BASE_URL}/list/suggestions`);
}


export async function getUser(): Promise<User> {
    return await get<User>(`${BASE_URL}/profile`);
}
export function niceShortName(event: Event) {
    let ESA_RE = /^esa([sw])(\d+)(?:s(\d+))?$/i;
    let match = event.short.match(ESA_RE);
    if (match) {
        let [_, season, year, stream_number] = match;
        let season_nice = season === "s" ? "Summer" : "Winter";
        if (stream_number) {
            return `ESA ${season_nice} ${year} S${stream_number}`;
        }
        return `ESA ${season_nice} ${year}`;
    }
    return event.short.toUpperCase();
}
