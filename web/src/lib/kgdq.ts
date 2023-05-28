import type {Run} from "vods.speedrun.club-client";

let API_DOMAIN = 'vods.speedrun.club';
export let BASE_URL= 'https://' + API_DOMAIN + '/api/v2';

export declare class VODSuggestion {
    url: string;
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