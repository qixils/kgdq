<script lang="ts">

    import {onMount} from "svelte";
    import type {User} from "$lib/kgdq.ts";
    import {BASE_URL, getUser} from "$lib/kgdq.ts";

    let user: User | null = null;
    let user_error: string | null = null;



    onMount(async () => {
        try {
            let data = await getUser();
            console.log(data);
        } catch (e) {
            user_error = e;
            console.error(e);
        }

        user = {"id":"01H1FNG7BG6VQX1ETDC3ZY13D6","name":"Dunkyl 🔣🔣"};

        localStorage.setItem("user", JSON.stringify(user));
    });
</script>

{#if user}
    <p>Logged in as {user.name}</p>
{:else}
    <p>Loading user...</p>
{/if}