import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// El front llama a su MISMO origen (localhost:5173) y Vite reenvía cada prefijo a su servicio.
// Así el navegador nunca ve otro origen: no hay CORS. Es un API Gateway en pequeño.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/auth-api': 'http://localhost:8081',
      '/match-api': 'http://localhost:8082',
      '/reservation-api': 'http://localhost:8083',
    },
  },
})