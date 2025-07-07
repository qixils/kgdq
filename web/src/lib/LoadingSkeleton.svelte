<script lang="ts">
    export let size = '30em';
</script>

<div class="outer" style="max-width: {size}">
    <div class="inner"></div>
</div>

<style>
    .outer {
        width: 2em;
        height: 2em;
        border-radius: 0.2em;
        border-radius: 0.7em;
        border-radius: 50%;
        /* pulsating background color */
        /* background: linear-gradient(90deg, hsla(0deg, 0%, 50%, 50%), hsla(0deg, 0%, 60%, 50%), hsla(0deg, 0%, 50%, 50%));
        background-size: 400% 400%; */
        animation: pulse 2s linear infinite;
        margin-inline: auto;
        /* background: linear-gradient(calc(var(--pulse)*360deg), 
            hsla(0, 0%, 40%, 50%),
            hsla(0deg, 0%, 60%, 50%),
            hsla(0deg, 0%, 40%, 50%)); */
        background: linear-gradient(calc(var(--pulse)*360deg), 
            hsl(329, 84%, 66%),
            hsl(0, 0%, 60%),
            hsl(113, 75%, 61%));
        
        /* border: 3px solid; */
        /* border-image-slice: 1; */
        padding: 4px;
        transform: rotate(45deg);
    }

    .inner {
        animation: pulse 5s ease-in-out infinite;
        background-color: color-mix(in srgb, var(--bg-bg) calc(abs(var(--pulse) - 0.5) * 30% + 70%), rgb(228, 84, 173));
        background-color: var(--bg-bg);
        width: 100%;
        height: 100%;
        border-radius: calc(0.7em - 4px);
        border-radius: 50%;
    }

    @property --pulse {
        syntax: '<number>';
        inherits: false;
        initial-value: 1;
    }

    @keyframes pulse {
        0% {
            /* background-position: 0 0; */
            --pulse: 0;
        }

        /* 50% {
            --pulse: 1;
        } */

        100% {
            --pulse: 1;
            /* background-position: 0 0; */
        }
    }
</style>
