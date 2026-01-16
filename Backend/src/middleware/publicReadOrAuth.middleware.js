// src/middleware/publicReadOrAuth.middleware.js
import { verifyAuth } from "./auth.middleware.js";

/**
 * Public-Read oder Auth:
 * - Erlaubt bestimmte Public Requests (nur READ + Login)
 * - Alles andere erfordert verifyAuth
 *
 * Funktioniert sowohl wenn:
 * - Caddy /api strippt (Pfad ist /initiatives)
 * - Caddy /api NICHT strippt (Pfad ist /api/initiatives)
 * - oder Backend selbst unter /api gemountet ist
 */
export function publicReadOrAuth(req, res, next) {
  const method = String(req.method || "").toUpperCase();
  const rawPath = req.path || req.originalUrl || "";

  // Normalisiere optionalen /api Prefix
  const path = rawPath.startsWith("/api/") ? rawPath.slice(4) : rawPath;

  // Public: Health
  if (method === "GET" && (path === "/health" || rawPath === "/health")) return next();

  // Public: Initiatives lesen
  if (method === "GET" && path.startsWith("/initiatives")) return next();

  // Public: Phase anzeigen
  if (method === "GET" && path === "/phases/current") return next();

  // Public: News lesen
  if (method === "GET" && path.startsWith("/news")) return next();

  // Public: Quiz lesen
  if (method === "GET" && path.startsWith("/quiz")) return next();

  // Public: Login
  if (method === "POST" && path === "/auth/login") return next();

  // Alles andere: protected
  return verifyAuth(req, res, next);
}
