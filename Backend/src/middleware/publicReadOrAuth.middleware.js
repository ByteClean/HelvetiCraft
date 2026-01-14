// src/middleware/publicReadOrAuth.middleware.js
import { verifyAuth } from "./auth.middleware.js";

/**
 * Public-Read oder Auth:
 * - Erlaubt bestimmte Public Requests (z.B. GET /initiatives, GET /phases/current)
 * - Alles andere erfordert verifyAuth (Minecraft/Discord/JWT)
 *
 * WICHTIG:
 * Da Caddy bei dir /api/* -> Backend weiterleitet und dabei /api stripped (handle_path),
 * sieht das Backend nur /initiatives, /phases/current usw. (ohne /api).
 */
export function publicReadOrAuth(req, res, next) {
  const method = (req.method || "").toUpperCase();
  const path = req.path || req.originalUrl || "";

  // --- Public endpoints (no auth) ---
  // Health
  if (method === "GET" && path === "/health") return next();

  // Initiatives: nur lesen (GET)
  if (method === "GET" && path.startsWith("/initiatives")) return next();

  // Phases current: nur lesen (GET)
  if (method === "GET" && path === "/phases/current") return next();

  // News: nur lesen (GET)
  if (method === "GET" && path.startsWith("/news")) return next();

  // Quiz: nur lesen (GET)
  if (method === "GET" && path.startsWith("/quiz")) return next();

  // Auth: Login muss ohne Auth gehen
  if (method === "POST" && path === "/auth/login") return next();

  // --- Everything else: protected ---
  return verifyAuth(req, res, next);
}
