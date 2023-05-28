<script lang="ts">
    export let size: string = '30em';
</script>

<div style="max-width: {size}"></div>

<style>
    div {
        width: 100%;
        height: 1em;
        border-radius: 0.2em;
        /* pulsating background color */
        background: linear-gradient(90deg, hsla(0deg, 0%, 50%, 50%), hsla(0deg, 0%, 60%, 50%), hsla(0deg, 0%, 50%, 50%));
        background-size: 400% 400%;
        animation: pulse 5s ease-in-out infinite;
    }

    @keyframes pulse {
        0% {
            background-position: 0 0;
        }

        50% {
            background-position: 100% 100%;
        }

        100% {
            background-position: 0 0;
        }
    }
</style>
