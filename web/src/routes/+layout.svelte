<script lang="ts">
  // import "../app.postcss";
  import "../app.css";
  import type {User} from "$lib/kgdq";
  import {page} from "$app/stores";
  import {onMount} from "svelte";

  let user: User | null = null;

  onMount(() => {
      let user_str = localStorage.getItem("user");
      user = user_str ? JSON.parse(user_str) : null;
  })
</script>

<header>
  <a href="/"><img src="/icon.svg" alt="Speedrun VOD Club" /></a>
  <a href="/"> <h1>Speedrun VOD Club</h1> </a>
  <nav>
    <ul>
<!--      <li><a href="/">Home</a></li>-->
      <li><a href="/events/upcoming">Upcoming Events</a></li>
      <li><a href="/events/recent">Recent Events</a></li>
      <li><a href="/organizations">Organizations</a></li>
        {#if user}
          <li>Logged in as <b>{ user.name }</b></li>
        {:else}
          <li><a href="/login">Log In to submit VODs</a></li>
        {/if}
    </ul>
  </nav>
</header>

<main>
  <slot />
</main>

<footer>
  <p>Copyright &copy; 2023 <a href="https://github.com/qixils">Lexi</a> and <a href="https://github.com/dunkyl">Dunkyl 🔣🔣</a></p>
  <p>This project is open source. Check it out <a href="https://github.com/qixils/kgdq">on GitHub</a>!</p>
  <p>Data generously provided by <a href="/organizations">the organizations that run the events</a>.</p>
</footer>
