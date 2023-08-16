import {meta} from "../../../stores";
import type {ServerLoad} from "@sveltejs/kit";

export const load = (async () => {
    meta.set({
        title: "Recent Events",
        description: "List of past marathon events."
    })
}) satisfies ServerLoad;

