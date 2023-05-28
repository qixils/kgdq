import type {Run} from "vods.speedrun.club-client";
import type {Event} from "vods.speedrun.club-client";

let API_DOMAIN = 'vods.speedrun.club';
export let BASE_URL= 'https://' + API_DOMAIN + '/api/v2';

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

export declare class DiscordUser {
    id: number;
    username: string;
    discriminator: string;
    avatar: string | null;
    bot: boolean;
    system: boolean;
    mfa_enabled: boolean;
    banner: string | null;
    accent_color: number | null;
    locale: string | null;
    verified: boolean;
    flags: number | null;
    premium_type: number | null;
    public_flags: number | null;
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

// get<T>(url: string | URL): Promise<T> {
//     // prepend the base URL if the URL is relative (i.e. a string)
//     const calculatedUrl = typeof url === 'string' ? `${this.baseUrl}/${url}` : url;
//     return this.fetchFunction(calculatedUrl).then(res => {
//         if (res.status >= 400) {
//             throw new Error(`Request failed with status code ${res.status}`);
//         }
//         return res.json();
//     });
// }

// let res = await fetch(`${BASE_URL}/profile`,
//     {
//         credentials: "include",
//         mode: 'cors',
//         headers: headers,
//     });
// let data = await res.json();
// console.log(data);
// // discord_user = data as DiscordUser;
// user = {"id":"01H1FNG7BG6VQX1ETDC3ZY13D6","name":"Dunkyl 🔣🔣"};
//
// localStorage.setItem("user", JSON.stringify(user));

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
