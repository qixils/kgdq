<script lang="ts">
  import "../app.css";
  import {user} from "../stores";
  import {onMount} from "svelte";

  onMount(() => {
    let user_local = localStorage.getItem("user");
    if (user_local) {
      $user = JSON.parse(user_local);
    }
  })
</script>

<header>
  <a href="/"><img src="/icon.svg" alt="Speedrun VOD Club" /></a>
  <a href="/"> <h1>Speedrun VOD Club</h1> </a>
  <nav>
    <ul>
      <li><a href="/events/upcoming">Upcoming Events</a></li>
      <li><a href="/events/recent">Recent Events</a></li>
      <li><a href="/organizations">Organizations</a></li>
        {#if $user}
          <li>Logged in as <b>{ $user.name }</b></li>
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
