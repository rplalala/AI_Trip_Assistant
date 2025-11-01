import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react-swc'

export default defineConfig(({mode}) => {
    const env = loadEnv(mode, process.cwd())

    return {
        plugins: [react()],
        server: {
            port: 5173,
            proxy: {
                '/api': { target: env.VITE_API_BASE, changeOrigin: true },
            },
        },
        define: {
            __API_BASE__: JSON.stringify(env.VITE_API_BASE),
        },
    }
})
