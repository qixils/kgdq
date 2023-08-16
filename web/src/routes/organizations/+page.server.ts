import {meta} from "../../stores";
import type {ServerLoad} from "@sveltejs/kit";

export const load = (async () => {
    meta.set({
        title: "Organizations",
        description: "Learn about the charity marathon organizations that host events covered by this site."
    })
}) satisfies ServerLoad;

