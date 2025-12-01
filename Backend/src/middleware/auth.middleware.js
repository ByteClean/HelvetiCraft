// src/middleware/auth.middleware.js

import jwt from "jsonwebtoken";
import pool from "../services/mysql.service.js";

const JWT_SECRET = process.env.JWT_SECRET;
const MINECRAFT_API_KEY = process.env.MINECRAFT_API_KEY;
const DISCORD_API_KEY = process.env.BOT_API_KEY;

/**
 * Einheitliche Authentifizierung für:
 * - Minecraft (Key + UUID)
 * - Discord Bot (Key + Discord-ID)
 * - Web-Login (JWT)
 */
export async function verifyAuth(req, res, next) {
  const origin = req.headers["x-auth-from"];     // minecraft | discord | web
  const key    = req.headers["x-auth-key"];      // Key für alle drei Quellen

  // Basisabsicherung
  if (!origin) {
    return res.status(400).json({ error: "missing_x-auth-from_header" });
  }
  if (!key) {
    return res.status(401).json({ error: "missing_x-auth-key_header" });
  }

  try {
    // -----------------------------------------------------------
    // 🟢 1. Minecraft – braucht: x-auth-from: minecraft
    //                         + x-auth-key (Key Vergleich)
    //                         + x-uuid (User-Mapping)
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
        "SELECT id, username, discord_id FROM authme WHERE uuid = ?",
        [uuid]
      );

      if (rows.length === 0) {
        return res.status(404).json({ error: "minecraft_user_not_found" });
      }

      req.user = {
        id: rows[0].id,
        username: rows[0].username,
        discord_id: rows[0].discord_id
      };
      req.source = "minecraft";
      return next();
    }


    // -----------------------------------------------------------
    // 🟣 2. Discord Bot – braucht: x-auth-from: discord
    //                         + x-auth-key (Bot-Key)
    //                         + x-discord-user (Discord-ID)
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
        "SELECT id, username FROM authme WHERE discord_id = ?",
        [discordId]
      );

      if (rows.length === 0) {
        return res.status(404).json({ error: "discord_user_not_linked" });
      }

      req.user = {
        id: rows[0].id,
        username: rows[0].username,
        discord_id: discordId
      };
      req.source = "discord";
      return next();
    }


    // -----------------------------------------------------------
    // 🟡 3. Web-Login – braucht: x-auth-from: web
    //                         + x-auth-key (JWT)
    // -----------------------------------------------------------
    if (origin === "web") {

      let payload;
      try {
        payload = jwt.verify(key, JWT_SECRET);
      } catch (err) {
        return res.status(401).json({ error: "invalid_or_expired_jwt" });
      }

      req.user = {
        id: payload.sub,
        username: payload.username
      };
      req.source = "web";
      return next();
    }


    // -----------------------------------------------------------
    // ❌ Unbekannte Quelle
    // -----------------------------------------------------------
    return res.status(400).json({ error: "unknown_origin" });

  } catch (err) {
    console.error("Auth Middleware Error:", err);
    return res.status(500).json({ error: "internal_auth_error" });
  }
}
