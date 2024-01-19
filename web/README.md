# Speedrun VOD Club Frontend

Frontend for [Speedrun VOD Club](https://vods.speedrun.club) with Svelte.

## Requirements

- Not in a symlinked directory
- NPM 9+
- Node 18+

## Other dependencies

`api-client-ts` is a typescript dependency that needs to be built.

```bash
# Run automatically by npm run dev or build
npm --prefix ../api-client-ts run build
```

## Run

```bash
npm install
npm run dev
```

## Build

```bash
npm run build
npm run preview
```