import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// 开发期将 /api 代理到本地后端（127.0.0.1:8080）；生产由 Nginx 同域反代
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1500,
  },
});
