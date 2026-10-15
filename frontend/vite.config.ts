import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://43.204.100.152:8080',
        changeOrigin: true,
      },
      '/actuator': {
        target: 'http://43.204.100.152:8080',
        changeOrigin: true,
      },
    },
  },
});
