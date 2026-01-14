// src/middleware/publicReadOrAuth.middleware.js
import { verifyAuth } from "./auth.middleware.js";

/**
 * Public-Read oder Auth:
 * - Erlaubt bestimmte Public Requests (nur READ + Login)
 * - Alles andere erfordert verifyAuth (minecraft/discord/web JWT)
 *
 * Hinweis: Bei dir strippt Caddy /api via handle_path /api/*,
 * daher sieht Express Pfade wie /initiatives statt /api/initiatives.
 */
export function publicReadOrAuth(req, res, next) {
  const method = String(req.method || "").toUpperCase();
  const path = req.path || req.originalUrl || "";

  // Public: Health
  if (method === "GET" && path === "/health") return next();

  // Public: Initiatives lesen
  if (method === "GET" && path.startsWith("/initiatives")) return next();

  // Public: Phase anzeigen
  if (method === "GET" && path === "/phases/current") return next();

  // Public: News lesen (falls du das willst)
  if (method === "GET" && path.startsWith("/news")) return next();

  // Public: Quiz lesen (falls du das willst)
  if (method === "GET" && path.startsWith("/quiz")) return next();

  // Public: Login (muss ohne Auth gehen)
  if (method === "POST" && path === "/auth/login") return next();

  // Alles andere: protected
  return verifyAuth(req, res, next);
}
