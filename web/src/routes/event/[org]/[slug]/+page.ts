import type {PageLoad} from './$types';
import {SvcClient} from "vods.speedrun.club-client";
import {BASE_URL} from "$lib/kgdq";

export const load = (async ({ fetch, params }) => {
    return { event: await new SvcClient(BASE_URL, fetch).getEvent(params.org, params.slug) };
}) satisfies PageLoad;