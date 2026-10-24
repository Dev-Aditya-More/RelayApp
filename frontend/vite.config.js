import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const api = process.env.VITE_PROXY_TARGET || 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5180, strictPort: true,
    proxy: { '/api': api, '/uploads': api },
  },
})
