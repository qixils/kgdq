<script lang="ts">
    import {BASE_URL} from "$lib/kgdq";
    import {page} from "$app/stores";
</script>

<h1>Log in for VOD Submission</h1>

<p>Videos are not hosted on this site, so we rely on volunteers to submit links to the runs that happen.</p>

<p>To prevent spam, please <a href="{BASE_URL}/auth/discord/callback?redirectUrl={$page.url.origin}/logged-in">log in with a Discord account</a>.</p>

<h2>Do you collect any data?</h2>

<p>Other than storing your VODs and verifying that you are logged in with a real Discord account when submitting, no data is collected.</p>