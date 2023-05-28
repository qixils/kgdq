<script lang="ts">
    import {page} from "$app/stores";

    export let title: string | null = null;

    export let description: string;

    export let url: string | null = null;

    export let noindex: boolean = false;

</script>

{#if title === null}
    <title>Speedrun VOD Club</title>
    <meta property="og:title" content="Speedrun VOD Club" />
{:else}
    <title>Speedrun VOD Club · { title }</title>
    <meta property="og:title" content={ title } />
    <meta property="og:site_name" content="Speedrun VOD Club" />
{/if}

<meta name="description" content="{ description }">
<meta property="og:description" content="{ description }" />

<meta property="og:url" content="{ url ?? $page.url }" />
<link rel="canonical" href="{ url ?? $page.url }">

{#if noindex}
    <meta name="robots" content="noindex">
{/if}
