import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// En desarrollo (npm run dev) las llamadas a /api van al backend de Spring Boot en el puerto 8080.
// "npm run build" deja la versión compilada dentro del backend, para que Spring Boot la sirva.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/h2-console': 'http://localhost:8080'
    }
  },
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true
  }
})
