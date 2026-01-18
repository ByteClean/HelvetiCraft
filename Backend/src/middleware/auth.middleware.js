// src/middleware/auth.middleware.js

import jwt from "jsonwebtoken";
import pool from "../services/mysql.service.js";

const JWT_SECRET = process.env.JWT_SECRET;
const MINECRAFT_API_KEY = process.env.MINECRAFT_API_KEY;
const DISCORD_API_KEY = process.env.BOT_API_KEY;

/**
 * Einheitliche Authentifizierung für:
 * - Minecraft (Key + UUID → authme.id)
 * - Discord Bot (Key + Discord-ID → authme.id)
 * - Web-Login (JWT → authme.id oder authme.username)
 *
 * Setzt IMMER:
 *   req.user = {
 *      id: <authme.id>,
 *      username: <string>,
 *      discord_id: <string|null>,
 *      isAdmin: true|false
 *   }
 */
export async function verifyAuth(req, res, next) {
  const origin = req.headers["x-auth-from"];     
  const key    = req.headers["x-auth-key"];      

  if (!origin) {
    return res.status(400).json({ error: "missing_x-auth-from_header" });
  }
  if (!key) {
    return res.status(401).json({ error: "missing_x-auth-key_header" });
  }

  try {

    // -----------------------------------------------------------
    // 1) Minecraft
    // -----------------------------------------------------------
    if (origin === "minecraft") {

      if (key !== MINECRAFT_API_KEY) {
        return res.status(403).json({ error: "invalid_minecraft_key" });
      }

      const uuid = req.headers["x-uuid"];
      if (!uuid) {
        return res.status(400).json({ error: "missing_x-uuid_header" });
      }

      const [rows] = await pool.query(
        "SELECT id, username, discord_id, isAdmin FROM authme WHERE uuid = ?",
        [uuid]
      );

      if (rows.length === 0) {
        return res.status(404).json({ error: "minecraft_user_not_found" });
      }

      const u = rows[0];

      req.user = {
        id: u.id,
        username: u.username,
        discord_id: u.discord_id || null,
        isAdmin: u.isAdmin === 1
      };

      req.source = "minecraft";
      return next();
    }

    // -----------------------------------------------------------
    // 2) Discord Bot
    // -----------------------------------------------------------
    if (origin === "discord") {

      if (key !== DISCORD_API_KEY) {
        return res.status(403).json({ error: "invalid_discord_key" });
      }

      const discordId = req.headers["x-discord-user"];
      if (!discordId) {
        return res.status(400).json({ error: "missing_x-discord-user_header" });
      }

      const [rows] = await pool.query(
        "SELECT id, username, uuid, discord_id, isAdmin FROM authme WHERE discord_id = ?",
        [discordId]
      );

      if (rows.length === 0) {
        return res.status(404).json({ error: "discord_user_not_linked" });
      }

      const u = rows[0];

      req.user = {
        id: u.id,
        username: u.username,
        discord_id: u.discord_id || null,
        isAdmin: u.isAdmin === 1
      };

      req.source = "discord";
      return next();
    }

   const WEBSITE_API_KEY = process.env.WEBSITE_API_KEY;

// ...

if (origin === "website") {
  if (key !== WEBSITE_API_KEY) {
    return res.status(403).json({ error: "invalid_website_key" });
  }

  req.user = {
    id: 0,
    username: "website",
    discord_id: null,
    isAdmin: false
  };

  req.source = "website";
  return next();
}


    // -----------------------------------------------------------
    // Unbekannte Quelle
    // -----------------------------------------------------------
    return res.status(400).json({ error: "unknown_origin" });

  } catch (err) {
    console.error("Auth Middleware Error:", err);
    return res.status(500).json({ error: "internal_auth_error" });
  }
}
