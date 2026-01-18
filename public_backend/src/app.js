import express from "express";
import cors from "cors";
import dotenv from "dotenv";
import { createProxyMiddleware } from "http-proxy-middleware";

dotenv.config();

const app = express();

app.use(cors());
app.use(express.json());

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

// Nur lesen
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
    },

    onError: (err, req, res) => {
      res.status(502).json({ message: "backend_unreachable", detail: err?.message });
    },
  })
);


export default app;
