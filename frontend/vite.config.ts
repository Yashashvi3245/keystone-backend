import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],

  server: {
    port: 5173,
    // Proxy /api/* and Swagger paths to the Spring Boot backend in development.
    // This removes the need for CORS configuration and matches the Docker nginx setup.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/swagger-ui': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/api-docs': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
