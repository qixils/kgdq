import type {MarathonEvent} from "vods.speedrun.club-client";

export const API_DOMAIN = 'vods.speedrun.club';
export const BASE_URL= 'https://' + API_DOMAIN + '/api/v2';

export const EPOCH = "1970-01-01T00:00:00Z";

export const ESA_RE = /^esa(w?)(\d+)(?:s(\d+))?$/i;

export function guessStreamNameAndUrl(org: string, eventSlug: string) {
    const esaMatch = ESA_RE.exec(eventSlug);
    let url = null;
    let name = null;
    if (esaMatch) {
        const streamSuffix = esaMatch[3] == '2' ? '2' : ''
        url = `https://twitch.tv/esamarathon${streamSuffix}`;
        name = `ESAMarathon${streamSuffix}`;
    } else if (org === 'gdq') {
        url =  `https://twitch.tv/gamesdonequick`;
        name = 'GamesDoneQuick';
    } else if (org === 'rpglb') {
        url =  `https://twitch.tv/rpglimitbreak`;
        name = "RPGLimitBreak";
    } else if (org === 'hek') {
        url = 'https://twitch.tv/esamarathon';
        name = "ESAMarathon";
    }
    return { streamName: `${name} on Twitch`, streamUrl: url, streamUsername: name };
}

export function compareEventStartTime(a: MarathonEvent, b: MarathonEvent) {
    return (a.startTime || EPOCH).localeCompare(b.startTime || EPOCH);
}

export function compareEventEndTime(a: MarathonEvent, b: MarathonEvent) {
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

export type Role = 'ADMIN' | 'MODERATOR' | 'APPROVED' | 'USER' | 'BANNED';

export declare class User {
    id: string;
    name: string;
    role: Role;
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
export function niceShortName(event: MarathonEvent) {
    let match = event.short.match(ESA_RE);
    if (match) {
        let [_, season, year, stream_number] = match;
        let season_nice = season === "w" ? "Winter" : "Summer";
        if (stream_number) {
            return `ESA ${season_nice} ${year} S${stream_number}`;
        }
        return `ESA ${season_nice} ${year}`;
    }
    return event.short.toUpperCase();
}
