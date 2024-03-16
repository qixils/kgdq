import { sveltekit } from '@sveltejs/kit/vite';
import type { UserConfig } from 'vite';
import {defineConfig} from "vite";

const common_config: UserConfig = {
	plugins: [sveltekit()]
};
export default defineConfig(({ command }) => {
	let config = common_config;
	if (command === 'serve') {
		config = {
			...config,
			server: {
				proxy: {
					'/api/v2': {
						target: 'https://vods.speedrun.club',
							changeOrigin: true,
							// secure: false,
							configure: (proxy, _options) => {
							proxy.on('error', (err, _req, _res) => {
								console.log('proxy error', err);
							});
							proxy.on('proxyReq', (proxyReq, req, _res) => {
								console.log('Sending Request to the Target:', req.method, req.url, req.headers);
							});
							proxy.on('proxyRes', (proxyRes, req, _res) => {
								console.log('Received Response from the Target:', proxyRes.statusCode, req.url, req.headers);
							});
						},
					}
				}
			}
		};
	}
	return config;
});
