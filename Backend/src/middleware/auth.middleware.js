// src/middleware/auth.middleware.js
import jwt from "jsonwebtoken";
import pool from "../services/mysql.service.js";

const JWT_SECRET = process.env.JWT_SECRET || "devsecret";
const BOT_API_KEY = process.env.BOT_API_KEY;
const MINECRAFT_API_KEY = process.env.MINECRAFT_API_KEY;

export async function verifyAuth(req, res, next) {
  const header = req.headers.authorization;

  if (!header) {
    return res.status(401).json({ error: "missing_authorization_header" });
  }

  try {
    /** 🟢 Minecraft → KEY, kein JWT */
    if (header.startsWith("Minecraft ")) {
      const key = header.slice(10).trim();
      if (key !== MINECRAFT_API_KEY) {
        return res.status(403).json({ error: "invalid_minecraft_key" });
      }
      req.source = "minecraft";
      return next();
    }

    /** 🟣 Discord → Bot KEY */
    if (header.startsWith("Bot ")) {
      const botKey = header.slice(4).trim();
      if (botKey !== BOT_API_KEY) {
        return res.status(403).json({ error: "invalid_bot_key" });
      }

      const discordId = req.headers["x-discord-user"];
      if (!discordId) {
        return res.status(400).json({ error: "missing_discord_user_header" });
      }

      const [rows] = await pool.query(
        "SELECT id, username FROM authme WHERE discord_id = ?",
        [discordId]
      );

      if (rows.length === 0) {
        return res.status(404).json({ error: "discord_user_not_linked" });
      }

      req.user = { id: rows[0].id, username: rows[0].username, discord_id: discordId };
      req.source = "discord";
      return next();
    }

    /** 🟡 Website → JWT */
    if (header.startsWith("Bearer ")) {
      const token = header.slice(7).trim();
      const payload = jwt.verify(token, JWT_SECRET);
      req.user = { id: payload.sub, username: payload.username };
      req.source = "web";
      return next();
    }

    /** ❌ Unbekannter Typ */
    return res.status(403).json({ error: "unsupported_auth_type" });
  } catch (err) {
    console.error(err);
    return res.status(401).json({ error: "invalid_or_expired_token" });
  }
}
