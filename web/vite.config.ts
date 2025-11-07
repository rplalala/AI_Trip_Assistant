import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'

/*
* 开发环境下 Vite 转发请求使前后端同源（生成环境下用 Nginx）
* 在 Vite 的 server.proxy 里，
* 只有以 /api 开头的请求才会被代理到 target；其它路径仍由 Vite 本地开发服务器处理。
*
* GET http://localhost:5173/api/test → 代理为 GET http://localhost:8082/api/test
* GET http://localhost:5173/auth/login → 不代理，仍由 Vite 处理
*
* */

export default defineConfig({
    plugins: [react()],
    server: {
        port: 5173,
        proxy: {
            '/api': { target: 'http://localhost:8082', changeOrigin: true },

            // 新增：本地把 /ext/geodb/* 代理到 GeoDB 的 HTTP free 实例
            '/ext/geodb': {
                target: 'http://geodb-free-service.wirefreethought.com',
                changeOrigin: true,
                // /ext/geodb/xxx  →  http://geodb.../v1/geo/xxx
                rewrite: (path) => path.replace(/^\/ext\/geodb/, '/v1/geo'),
            },
            '/ext/unsplash': {
                target: 'https://images.unsplash.com',
                changeOrigin: true,
                secure: true, // 上游是 HTTPS，校验证书
                headers: {
                    // 有些上游对 Host/SNI 严格，手动对齐最保险
                    Host: 'images.unsplash.com',
                },
                // 去掉本地前缀，保持与生产 Nginx 的映射一致
                rewrite: (path) => path.replace(/^\/ext\/unsplash\/?/, '/'),
            },
        },
    },
})