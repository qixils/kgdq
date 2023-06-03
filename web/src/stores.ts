import {writable} from "svelte/store";
import type { Writable } from "svelte/store";
import type {User} from "$lib/kgdq";


export const user: Writable<({ expires?: number } & User) | null> = writable(null);