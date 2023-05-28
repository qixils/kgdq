<script lang="ts">

    import {onMount} from "svelte";
    import type {User} from "$lib/kgdq.ts";
    import {BASE_URL, getUser} from "$lib/kgdq.ts";

    let user: User | null = null;
    let user_error: string | null = null;



    onMount(async () => {
        try {
            user = await getUser();
        } catch (e) {
            user_error = e;
            console.error(e);
        }

        localStorage.setItem("user", JSON.stringify(user));
    });
</script>

{#if user}
    <p>Logged in as {user.name}</p>
{:else if user_error}
    <p>Error loading user: {user_error}</p>
{:else}
    <p>Loading user...</p>
{/if}