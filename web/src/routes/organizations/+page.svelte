<script lang="ts">
    import {IdentifiedOrganization} from "vods.speedrun.club-client";

    export let data: { orgs: IdentifiedOrganization[] };
</script>

<svelte:head>
    <title>Speedrun VOD Club - Organizations</title>
    <meta name="description" content="TODO KGDQ meta tags" />
</svelte:head>

<h1>Organizations</h1>

<p>Speedruns and events on this site are all from a selection of charity speedrun events.</p>

{#each data.orgs as org (org.id)}
    <section>
        <h2>{org.displayName}</h2>
        {#if org.id === "gdq"}
            <p>Commonly known as GDQ, very popular and hosts <abbr title="Awesome Games Done Quick">AGDQ</abbr> and <abbr title="Summer Games Done Quick">SGDQ</abbr> every winter and summer in the US.</p>
        {:else if org.id === "esa"}
            <p>ESA also hosts two annual events, ESA Winter and ESA Summer, in Sweden.</p>
        {:else if org.id === "hek"}
            <p>TODO</p>
        {:else if org.id === "rpglb"}
            <p>TODO</p>
        {:else}
            <p>We don't know much about this organization yet but we're sure they're lovely and you should support them.</p>
        {/if}
        <ul>
            <li><a href={org.homepageUrl} target="_blank">{org.shortName} Homepage</a></li>
            <li><a href="/event/{org.id}">View {org.shortName}-hosted events</a></li>
        </ul>
    </section>
{/each}
