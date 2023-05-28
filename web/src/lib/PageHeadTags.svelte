<script lang="ts">
    import {page} from "$app/stores";

    export let title: string | null = null;

    export let description: string;

    export let url: string | null = null;

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