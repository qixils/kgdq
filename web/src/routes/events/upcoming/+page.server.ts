import {meta} from "../../../stores";
import type {ServerLoad} from "@sveltejs/kit";

export const load = (async () => {
    meta.set({
        title: "Upcoming Events",
        description: "List of marathon events that will happen in the future."
    })
}) satisfies ServerLoad;

