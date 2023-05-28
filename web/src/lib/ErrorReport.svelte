<script lang="ts">
    export let message: string;

    export let icon = "error";
</script>

<div class="error-box">
    <div class="material-symbols-rounded error-icon">{ icon }</div>
    <h2>Error!</h2>
    <p class="error-message">{ message }</p>
</div>