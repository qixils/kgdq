import {meta} from "../../../../stores";
import type {PageServerLoad} from "./$types";
import {SvcClient} from "vods.speedrun.club-client";
import {BASE_URL, niceShortName} from "$lib/kgdq";

export const load = (async ({ fetch, params }) => {
    const data = { event: await new SvcClient(BASE_URL, fetch).getEvent(params.org.toLowerCase(), params.slug, true) }
    meta.set({
        title: data.event.name,
        description: `View the schedule of ${niceShortName(data.event)} and watch back the VODs.`
    })
    return data
}) satisfies PageServerLoad;

