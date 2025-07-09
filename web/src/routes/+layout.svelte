<script lang="ts">
  import "../app.css";
  import {meta, user} from "../stores";
  import {onMount} from "svelte";
  import {page} from "$app/stores";


  onMount(async () => {
    let user_local = localStorage.getItem("user");
    if (user_local) {
      $user = JSON.parse(user_local);
    }
    if ($user && ($user.expires === undefined || $user.expires < Date.now())) {
      //
      $user = null;
      localStorage.removeItem("user");
    }
  })
</script>

<svelte:head>
  {#if $meta.title}
    <title>Speedrun VOD Club · { $meta.title }</title>
    <meta property="og:title" content="Speedrun VOD Club · { $meta.title }" />
    <meta property="og:site_name" content="Speedrun VOD Club" />
  {:else}
    <title>Speedrun VOD Club</title>
    <meta property="og:title" content="Speedrun VOD Club" />
  {/if}

  <meta name="description" content="{ $meta.description }">
  <meta property="og:description" content="{ $meta.description }" />

  <meta property="og:url" content="{ $meta.url ?? $page.url }" />
  <link rel="canonical" href="{ $meta.url ?? $page.url.toString() }">

  {#if $meta.noindex}
    <meta name="robots" content="noindex">
  {/if}
</svelte:head>

<header>
  <a href="/"><img src="/icon.svg" alt="Speedrun VOD Club" width="80px" height="60.8px" /></a>
  <a href="/"> <h1>Speedrun VOD Club</h1> </a>
  <nav>
    <ul>
      <li><a href="/events/upcoming">Upcoming Events</a></li>
      <li><a href="/events/recent">Recent Events</a></li>
      <li><a href="/organizations">Organizations</a></li>
        {#if $user}
          <li>Logged in as <b>{ $user.name }</b></li>
        {:else}
          <li><a href="/login">Log in to submit VODs</a></li>
        {/if}
    </ul>
  </nav>
</header>

<main>
  <slot />
</main>

<footer>
  <p>Copyright &copy; 2025 <a href="https://github.com/qixils">Lexi</a> and <a href="https://github.com/dunkyl">Dunkyl 🔣🔣</a></p>
  <p>This project is open source. Check it out <a href="https://github.com/qixils/kgdq">on GitHub</a>!</p>
  <p>Data generously provided by <a href="/organizations">the organizations that run the events</a>.</p>
</footer>
