
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
      '/api': {
        target: process.env.VITE_API_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
        // Rewrite the cookie domain to the dev origin so the browser stores it.
        cookieDomainRewrite: '',
      },
    },
  },
})
