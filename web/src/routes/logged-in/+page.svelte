<script lang="ts">

    import {onMount} from "svelte";
    import {getUser} from "$lib/kgdq.ts";
    import {user} from "../../stores";
    import ErrorReport from "$lib/ErrorReport.svelte";

    let user_error: string | null = null;

    onMount(async () => {
        try {
            $user = await getUser();
            // TODO: api should tell when the token expires other than in the cookie
            // related: token refreshes
            let week = 1000 * 60 * 60 * 24 * 7;
            localStorage.setItem("user", JSON.stringify({ ...$user, expires: Date.now() + week }));
        } catch (e) {
            user_error = e;
            console.error(e);
        }
    });
</script>

{#if $user}
    <p>Logged in as {$user.name}</p>
{:else if user_error}
    <ErrorReport
            message="Error loading user: {user_error}" />
{:else}
    <p>Loading user...</p>
{/if}