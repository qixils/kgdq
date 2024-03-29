import {meta} from "../../stores";
import type {Load} from "@sveltejs/kit";

export const load = (async () => {
    meta.set({
        title: "Log In Success",
        description: "Check your account status.",
        noindex: true
    })
}) satisfies Load;

