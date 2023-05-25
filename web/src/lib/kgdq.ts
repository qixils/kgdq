import type {Run} from "vods.speedrun.club-client";

let API_DOMAIN = 'vods.speedrun.club';
export let BASE_URL= 'https://' + API_DOMAIN + '/api/v2';

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