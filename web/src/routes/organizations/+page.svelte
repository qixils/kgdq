<script lang="ts">
    import type {Organization} from "vods.speedrun.club-client";
    import PageHeadTags from "$lib/PageHeadTags.svelte";
    import ErrorReport from "$lib/ErrorReport.svelte";

    export let data: { orgs: Organization[] | Error };
</script>

<svelte:head>
    <PageHeadTags
            title="Organizations"
            description="Learn about the charity marathon organizations that host events covered by this site." />
</svelte:head>

<h1>Organizations</h1>

<p>Speedruns and events on this site are all from a selection of charity speedrun events.</p>

{#if data.orgs instanceof Error}
    <ErrorReport
        message="Failed to load organizations: {data.orgs.message}" />
{:else if data.orgs.length === 0}
    <p>No organizations found.</p>
{:else}
    {#each data.orgs as org (org.id)}
        <section>
            <h2>{org.displayName}</h2>
            <p>{#if org.id === "gdq"}
                Also known as GDQ, this highly popular American organization hosts two main week-long events each year,
                <abbr title="Awesome Games Done Quick">AGDQ</abbr> and <abbr title="Summer Games Done Quick">SGDQ</abbr>.
                The former is held in the winter for the benefit of the
                <a href="https://preventcancer.org/">Prevent Cancer Foundation</a> while the latter is held in the summer
                for the benefit of <a href="https://doctorswithoutborders.org/">Doctors Without Borders</a>.
                GDQ also hosts two Frame Fatales events made by and for women and non-binary speedrunners each year for the
                benefit of the <a href="https://malala.org/">Malala Fund</a>.
            {:else if org.id === "esa"}
                ESA is the largest European speedrunning marathon and holds two events in Sweden each year. ESA Winter
                benefits <a href="https://savethechildren.org/">Save the Children</a> while ESA Summer benefits
                <a href="https://www.alzheimersgameover.com/">Alzheimerfonden</a>.
            {:else if org.id === "hek"}
                A spin-off of ESA started by the titular Hekigan, this incredibly silly marathon raises money for the same
                charities as ESA each year.
            {:else if org.id === "rpglb"}
                RPGLB holds an annual event all about RPGs for the benefit of the
                <a href="https://www.nami.org/">National Institute on Mental Illness</a>.
            {:else}
                We don't know much about this organization yet but we're sure they're lovely and you should support them.
            {/if}</p>
            <ul>
                <li><a href={org.homepageUrl} target="_blank">{org.shortName} Homepage</a></li>
                <li><a href="/event/{org.id}">View {org.shortName}-hosted events</a></li>
            </ul>
        </section>
    {/each}
{/if}