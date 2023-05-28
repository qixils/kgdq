import type {Run} from "vods.speedrun.club-client";

let API_DOMAIN = 'vods.speedrun.club';
export let BASE_URL= 'https://' + API_DOMAIN + '/api/v2';

export declare class VODSuggestion {
    url: string;
    organization: string;
    gdqId: number | null;
    horaroId: string | null;
}


export declare class VodLink {
    type: "YOUTUBE" | "TWITCH" | "OTHER";
    videoId: string;
    timestamp: number | null;
    url: string;
    contributorId: string;
}
export declare class VODSuggestion2 {
    vod: VodLink;
    organization: string;
    gdqId: number | null;
    horaroId: string | null;
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

export async function getSuggestions(): Promise<VODSuggestion2[]> {
    return await get<VODSuggestion2[]>(`${BASE_URL}/list/suggestions`);
}


export async function getUser(): Promise<User> {
    return await get<User>(`${BASE_URL}/profile`);
}