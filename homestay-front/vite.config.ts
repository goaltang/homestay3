import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import path from "path";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        secure: false,
        ws: true,
        configure: (proxy, _options) => {
          proxy.on("error", (err, _req, _res) => {
            console.log("代理错误:", err);
          });
          proxy.on("proxyReq", (proxyReq, req, _res) => {
            console.log("发送代理请求头:", proxyReq.getHeaders());

            let body = "";
            req.on("data", (chunk) => {
              body += chunk;
            });
            req.on("end", () => {
              if (body) {
                console.log("发送代理请求体:", body);
              }
            });
          });
          proxy.on("proxyRes", (proxyRes, req, _res) => {
            let body = "";
            proxyRes.on("data", (chunk) => {
              body += chunk;
            });
            proxyRes.on("end", () => {
              console.log("代理响应:", {
                statusCode: proxyRes.statusCode,
                url: req.url,
                headers: proxyRes.headers,
                body: body,
              });
            });
          });
        },
      },
    },
  },
});
