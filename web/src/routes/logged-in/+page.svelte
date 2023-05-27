<script lang="ts">

    import {onMount} from "svelte";
    import type {DiscordUser} from "$lib/kgdq.ts";
    import {BASE_URL} from "$lib/kgdq.ts";

    let discord_user: DiscordUser | null = null;
    let discord_error: string | null = null;

    onMount(async () => {
        let res = await fetch(`${BASE_URL}/auth/user`, { credentials: "include" });
        let data = await res.json();
        discord_user = data as DiscordUser;
    });
</script>

{#if discord_user}
    <p>Logged in as {discord_user.username}#{discord_user.discriminator}</p>
{:else}
    <p>Loading user...</p>
{/if}