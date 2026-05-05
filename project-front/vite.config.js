import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import {fileURLToPath, URL} from 'node:url'
import http from 'node:http'

export default defineConfig({
  plugins: [
    vue(),
    // 自定义中间件：处理 SSE 流式代理，绕过 http-proxy 的缓冲问题
    {
      name: 'sse-proxy-middleware',
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          // 只拦截 AI 聊天流式接口
          if (!req.url?.startsWith('/api/ai/chat/messages')) {
            return next()
          }

          // 收集请求体（POST）
          const chunks = []
          req.on('data', chunk => chunks.push(chunk))
          req.on('end', () => {
            const body = Buffer.concat(chunks).toString()
            const headers = {
              'Content-Type': req.headers['content-type'] || 'application/json',
              'Accept': 'text/event-stream'
            }
            if (req.headers['token']) {
              headers['token'] = req.headers['token']
            }

            // 用 Node.js 原生 http 模块直连后端，逐块转发
            const proxyReq = http.request({
              hostname: 'localhost',
              port: 8080,
              path: req.url,
              method: req.method,
              headers
            }, (proxyRes) => {
              res.statusCode = proxyRes.statusCode
              // 逐块转发，不缓冲
              proxyRes.on('data', (chunk) => {
                res.write(chunk)
              })
              proxyRes.on('end', () => {
                res.end()
              })
            })

            proxyReq.on('error', (err) => {
              console.error('SSE proxy error:', err)
              if (!res.headersSent) {
                res.statusCode = 502
              }
              res.end()
            })

            if (body) {
              proxyReq.write(body)
            }
            proxyReq.end()
          })
        })
      }
    }
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 3000,
    host: true,
    proxy: {
      // 通用 API 代理
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // WebSocket 代理
      '/ws': {
        target: 'http://localhost:8080/api',
        ws: true,
        changeOrigin: true
      }
    }
  }
})
