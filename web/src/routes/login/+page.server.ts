import {meta} from "../../stores";
import type {ServerLoad} from "@sveltejs/kit";

export const load = (async () => {
    meta.set({
        title: "Log In",
        description: "Log in to submit VODs to appear on the website.",
        noindex: true
    })
}) satisfies ServerLoad;

