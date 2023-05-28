import type {PageLoad} from './$types';
import {SvcClient} from "vods.speedrun.club-client";
import {BASE_URL} from "$lib/kgdq";

export const load = (async ({ fetch, params }) => {
    return { org: await new SvcClient(BASE_URL, fetch).getMarathon(params.org.toLowerCase(), false) };
}) satisfies PageLoad;
