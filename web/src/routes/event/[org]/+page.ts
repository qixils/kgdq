import {SvcClient} from "vods.speedrun.club-client";
import type {PageLoad} from './$types';

const svc = new SvcClient();

export const load = (async ({ params }) => {
    return { org: await svc.getMarathon(params.org, false) };
}) satisfies PageLoad;
