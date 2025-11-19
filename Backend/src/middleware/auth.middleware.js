import jwt from "jsonwebtoken";
import pool from "../services/mysql.service.js";

const JWT_SECRET = process.env.JWT_SECRET || "devsecret";
const BOT_API_KEY = process.env.BOT_API_KEY || "supersecretbotkey";

// 🔸 Für reine JWT-geschützte Routen (Website / Minecraft)
export function requireAuth(req, res, next) {
  const auth = req.headers.authorization;
  if (!auth || !auth.startsWith("Bearer "))
    return res.status(401).json({ error: "missing_token" });

  const token = auth.slice(7);
  try {
    const payload = jwt.verify(token, JWT_SECRET);
    req.user = payload; // { sub, username }
    next();
  } catch (err) {
    res.status(401).json({ error: "invalid_token" });
  }
}

// 🔹 Für gemischte Quellen (JWT oder Discord Bot)
export async function identifySource(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader)
    return res.status(401).json({ error: "missing_authorization_header" });

  try {
    // 🟢 JWT (Minecraft / Website)
    if (authHeader.startsWith("Bearer ")) {
      const token = authHeader.slice(7);
      const payload = jwt.verify(token, JWT_SECRET);
      req.user = { id: payload.sub, username: payload.username, source: "minecraft_or_web" };
      return next();
    }

    // 🟣 Discord Bot
    if (authHeader.startsWith("Bot ")) {
      const botKey = authHeader.slice(4).trim();
      if (botKey !== BOT_API_KEY)
        return res.status(403).json({ error: "invalid_bot_token" });

      const discordId = req.headers["x-discord-user"];
      if (!discordId)
        return res.status(400).json({ error: "missing_discord_user_header" });

      const [rows] = await pool.query(
        "SELECT id, username FROM authme WHERE discord_id = ?",
        [discordId]
      );

      if (rows.length === 0)
        return res.status(404).json({ error: "discord_user_not_linked" });

      req.user = { id: rows[0].id, username: rows[0].username, discord_id: discordId, source: "discord" };
      return next();
    }

    // ❌ Unbekannter Typ
    res.status(401).json({ error: "unsupported_auth_type" });
  } catch (err) {
    console.error(err);
    res.status(401).json({ error: "invalid_or_expired_token" });
  }
}
