import {meta} from "../../stores";
import type {Load} from "@sveltejs/kit";

export const load = (async () => {
    meta.set({
        noindex: true,
        title: "Admin >:3",
        description: "Modify event data and approve VOD suggestions."
    })
}) satisfies Load;

