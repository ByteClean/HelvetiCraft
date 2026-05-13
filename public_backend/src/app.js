import express from "express";
import cors from "cors";
import dotenv from "dotenv";
import { createProxyMiddleware } from "http-proxy-middleware";
import publicRoutes from "./routes/public.routes.js";

dotenv.config();

const app = express();

app.use(cors());
app.use(express.json());

// Mount public routes directly (including login)
app.use(publicRoutes);

const BACKEND_BASE_URL = process.env.BACKEND_BASE_URL;
if (!BACKEND_BASE_URL) {
  throw new Error("BACKEND_BASE_URL missing");
}

// Website-Identitaet
const PUBLIC_X_AUTH_FROM = "website";
const PUBLIC_X_AUTH_KEY = process.env.PUBLIC_X_AUTH_KEY;

if (!PUBLIC_X_AUTH_KEY) {
  throw new Error("PUBLIC_X_AUTH_KEY missing");
}

// Allow only GET/HEAD requests to be proxied to backend (POST/PUT/DELETE blocked)
app.use((req, res, next) => {
  if (req.method !== "GET" && req.method !== "HEAD") {
    return res.status(405).json({ message: "method_not_allowed" });
  }
  next();
});

// absichtlich nicht public
app.get("/quiz/question", (req, res) => {
  res.status(404).json({ message: "not_found" });
});

app.use(
  "/",
  createProxyMiddleware({
    target: BACKEND_BASE_URL,
    changeOrigin: true,
    logLevel: "debug",

    // garantiert beim Request dabei
    headers: {
      "x-auth-from": "website",
      "x-auth-key": process.env.PUBLIC_X_AUTH_KEY,
    },

    onProxyReq: (proxyReq, req, res) => {
      proxyReq.setHeader("x-auth-from", "website");
      proxyReq.setHeader("x-auth-key", process.env.PUBLIC_X_AUTH_KEY);
      
      // Explicitly forward request body for POST/PUT/PATCH
      if (req.method !== "GET" && req.body) {
        const bodyData = JSON.stringify(req.body);
        proxyReq.setHeader("Content-Type", "application/json");
        proxyReq.setHeader("Content-Length", Buffer.byteLength(bodyData));
        proxyReq.write(bodyData);
      }
    },

    onError: (err, req, res) => {
      console.error("Proxy Error:", err.message, "Path:", req.path, "Method:", req.method);
      res.status(502).json({ message: "backend_unreachable", detail: err?.message });
    },

    timeout: 30000,
  })
);


export default app;
