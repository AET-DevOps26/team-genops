
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '~': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    // Proxy API calls to the auth service so the browser sees one origin.
    // That keeps the HttpOnly session cookies same-site (SameSite=Strict works)
    // without CORS — mirroring how the gateway serves both in production.
    proxy: {
      // More specific rule first — genai service on port 8000
      '/api/v1/chat': {
        target: process.env.VITE_GENAI_TARGET ?? 'http://localhost:8000',
        changeOrigin: true,
      },
      // Catch-all — auth service on port 8080
      '/api': {
        target: process.env.VITE_API_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
        cookieDomainRewrite: '',
      },
    },
  },
})
