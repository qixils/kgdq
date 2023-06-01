import type {PageLoad} from './$types';
import {SvcClient} from "vods.speedrun.club-client";
import {BASE_URL} from "$lib/kgdq";

export const load = (async ({ fetch }) => {
    try {
        return { orgs: await new SvcClient(BASE_URL, fetch).getMarathons(false) };
    } catch (e) {
        return { orgs: e };
    }
}) satisfies PageLoad;
