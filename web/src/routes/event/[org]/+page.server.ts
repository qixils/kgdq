import {meta} from "../../../stores";
import type {PageServerLoad} from './$types';
import {SvcClient} from "vods.speedrun.club-client";
import {BASE_URL} from "$lib/kgdq";

export const load = (async ({ fetch, params }) => {
    const data = { org: await new SvcClient(BASE_URL, fetch).getMarathon(params.org.toLowerCase(), false) };
    meta.set({
        title: `${data.org.displayName} Events`,
        description: `List of events hosted by ${data.org.displayName}.`
    })
    return data
}) satisfies PageServerLoad;

