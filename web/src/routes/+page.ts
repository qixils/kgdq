import {meta} from "../stores";
import type {Load} from "@sveltejs/kit";

export const load = (async () => {
    meta.set({
        description: "Find VODs of speedruns from charity marathon streams.",
        url: "https://vods.speedrun.club"
    })
}) satisfies Load;
