import {SvcClient} from "vods.speedrun.club-client";

const svc = new SvcClient();

export async function load(params: {org: string}) {
    return { org: await svc.getMarathon(params.org, false) };
}
