<script lang="ts">
    import {page} from "$app/stores";

    export let title: string | null = null;

    export let description: string;

    export let url: string | null = null;

    export let no_index: boolean = false;

</script>

{#if title === null}
    <title>Speedrun VOD Club</title>
    <meta property="og:title" content="Speedrun VOD Club" />
{:else}
    <title>Speedrun VOD Club · { title }</title>
    <meta property="og:title" content={ title } />
{/if}

<meta name="description" content="{ description }">
<meta property="og:description" content="{ description }" />

<meta property="og:url" content="{ url ?? $page.url }" />
<link rel="canonical" href="{ url ?? $page.url }">

{#if no_index}
    <meta name="robots" content="noindex">
{/if}
