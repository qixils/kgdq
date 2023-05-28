<script lang="ts">
    import {BASE_URL} from "$lib/kgdq.ts";
    import PageHeadTags from "$lib/PageHeadTags.svelte";

    let current_url = window.location.href;
    let redirect_url = current_url.replace("/login", "/logged-in");
</script>

<svelte:head>
    <PageHeadTags
            title="Log In"
            description="Log in to submit VODs to appear on the website." />
</svelte:head>

<h1>Log in for VOD Submission</h1>

<p>Videos are not hosted on this site, so we rely on volunteers to submit links to the runs that happen.</p>

<p>To prevent spam, please <a href="{ `${BASE_URL}/auth/discord/callback?redirectUrl=${redirect_url}` }">log in with a Discord account</a>.</p>

<h2>Do you collect any data?</h2>

<p>Other than storing your VODs and verifying that you are logged in with a real Discord account when submitting, no data is collected.</p>