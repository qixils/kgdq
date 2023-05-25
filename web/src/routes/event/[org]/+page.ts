import {SvcClient} from "vods.speedrun.club-client";
import type {PageLoad} from './$types';
import {BASE_URL} from "$lib/kgdq";

const svc = new SvcClient(BASE_URL);

export const load = (async ({ params }) => {
    return { org: await svc.getMarathon(params.org, false) };
}) satisfies PageLoad;
