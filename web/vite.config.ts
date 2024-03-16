import { sveltekit } from '@sveltejs/kit/vite';
import type { UserConfig } from 'vite';
import {defineConfig} from "vite";

const config: UserConfig = {
	plugins: [sveltekit()],
	server: {
		proxy: {
			'/api/v2': {
				target: 'https://vods.speedrun.club',
					changeOrigin: true,
			}
		}
	}
};

export default config;
