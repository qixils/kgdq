<script lang="ts">

    import {onMount} from "svelte";
    import {getUser} from "$lib/kgdq.ts";
    import {user} from "../../stores";
    import PageHeadTags from "$lib/PageHeadTags.svelte";

    let user_error: string | null = null;

    onMount(async () => {
        try {
            $user = await getUser();
            localStorage.setItem("user", JSON.stringify($user));
        } catch (e) {
            user_error = e;
            console.error(e);
        }
    });
</script>

<svelte:head>
    <PageHeadTags
            no_index={true}
            title="Log In Success"
            description="Check your account status." />
</svelte:head>

{#if $user}
    <p>Logged in as {$user.name}</p>
{:else if user_error}
    <p>Error loading user: {user_error}</p>
{:else}
    <p>Loading user...</p>
{/if}