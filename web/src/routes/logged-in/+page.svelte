<script lang="ts">

    import {onMount} from "svelte";
    import type {User} from "$lib/kgdq.ts";
    import {BASE_URL, getUser} from "$lib/kgdq.ts";
    import {user} from "../../stores";
    import PageHeadTags from "$lib/PageHeadTags.svelte";

    // let user: User | null = null;
    let user_error: string | null = null;



    onMount(async () => {
        try {
            $user = await getUser();
        } catch (e) {
            user_error = e;
            console.error(e);
        }
    });
</script>

<svelte:head>
    <PageHeadTags
            title="Log In Success"
            description="" />
</svelte:head>

{#if $user}
    <p>Logged in as {$user.name}</p>
{:else if user_error}
    <p>Error loading user: {user_error}</p>
{:else}
    <p>Loading user...</p>
{/if}