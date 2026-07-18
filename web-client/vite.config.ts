import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { fileURLToPath, URL } from "node:url";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      "~": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    // Proxy API calls to the auth service so the browser sees one origin.
    // That keeps the HttpOnly session cookies same-site (SameSite=Strict works)
    // without CORS — mirroring how the gateway serves both in production.
    proxy: {
      // More specific rules first — genai service on port 8000
      "/api/v1/chat": {
        target: process.env.VITE_GENAI_TARGET ?? "http://localhost:8000",
        changeOrigin: true,
      },
      "/api/v1/job-postings": {
        target: process.env.VITE_GENAI_TARGET ?? "http://localhost:8000",
        changeOrigin: true,
      },
      // Application service on port 8082
      "/api/v1/applications": {
        target: process.env.VITE_APPLICATION_TARGET ?? "http://localhost:8082",
        changeOrigin: true,
      },
      // Document service (profile + generated documents) on port 8083
      "/api/v1/profile": {
        target: process.env.VITE_DOCUMENT_TARGET ?? "http://localhost:8083",
        changeOrigin: true,
      },
      "/api/v1/documents": {
        target: process.env.VITE_DOCUMENT_TARGET ?? "http://localhost:8083",
        changeOrigin: true,
      },
      // Catch-all — auth service on port 8080
      "/api": {
        target: process.env.VITE_API_TARGET ?? "http://localhost:8080",
        changeOrigin: true,
        cookieDomainRewrite: "",
      },
    },
  },
});
