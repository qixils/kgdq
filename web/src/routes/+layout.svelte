<script lang="ts">
  // import "../app.postcss";
  import "../app.css";
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
      <li><a id="log-in" href="/login">Log In to submit VODs</a></li>
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
