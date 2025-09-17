import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    allowedHosts: [
      'helveticraft.com',
      'localhost', // Keep localhost for local testing
      // Add any other hosts you want to allow
    ],
  },
})
