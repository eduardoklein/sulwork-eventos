/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// In development the browser calls the API on the frontend's own address, and Vite forwards it to the backend.
// In Docker, nginx plays the same role, so the code never changes between environments.
const backend = 'http://localhost:8080'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api': backend,
      '/swagger-ui': backend,
      '/v3/api-docs': backend,
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
  },
})
