<script lang="ts">

    import {onMount} from "svelte";
    import type {User} from "$lib/kgdq.ts";
    import {BASE_URL} from "$lib/kgdq.ts";

    let user: User | null = null;
    let user_error: string | null = null;

    onMount(async () => {
        let res = await fetch(`${BASE_URL}/profile`, { credentials: "include" });
        let data = await res.json();
        console.log(data);
        // discord_user = data as DiscordUser;
        user = {"id":"01H1FNG7BG6VQX1ETDC3ZY13D6","name":"Dunkyl 🔣🔣"};

        localStorage.setItem("user", JSON.stringify(user));
    });
</script>

{#if user}
    <p>Logged in as {user.name}</p>
{:else}
    <p>Loading user...</p>
{/if}