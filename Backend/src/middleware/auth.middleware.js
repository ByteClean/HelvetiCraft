// src/middleware/auth.middleware.js
import jwt from "jsonwebtoken";
import pool from "../services/mysql.service.js";

const JWT_SECRET = process.env.JWT_SECRET;
const BOT_API_KEY = process.env.BOT_API_KEY;
const MINECRAFT_API_KEY = process.env.MINECRAFT_API_KEY;

export async function verifyAuth(req, res, next) {

  const origin = req.headers["x-auth-from"];
  const auth = req.headers.authorization;

  if (!origin) {
    return res.status(400).json({ error: "missing_x-auth-from_header" });
  }

  if (!auth) {
    return res.status(401).json({ error: "missing_authorization_header" });
  }


  try {

    // 🟢 1. Minecraft
    if (origin === "minecraft") {

      if (!auth.startsWith("Minecraft ")) {
        return res.status(403).json({ error: "invalid_auth_scheme_for_minecraft" });
      }

      const key = auth.slice(10).trim();
      if (key !== MINECRAFT_API_KEY) {
        return res.status(403).json({ error: "invalid_minecraft_key" });
      }

      const uuid = req.headers["x-uuid"];
      if (!uuid) {
        return res.status(400).json({ error: "missing_minecraft_uuid" });
      }

      const [rows] = await pool.query(
        "SELECT id, username, discord_id FROM authme WHERE uuid = ?",
        [uuid]
      );

      if (rows.length === 0) {
        return res.status(404).json({ error: "minecraft_user_not_found" });
      }

      req.user = rows[0];
      req.source = "minecraft";
      return next();
    }


    // 🟣 2. Discord Bot
    if (origin === "discord") {

      if (!auth.startsWith("Bot ")) {
        return res.status(403).json({ error: "invalid_auth_scheme_for_discord" });
      }

      const key = auth.slice(4).trim();
      if (key !== BOT_API_KEY) {
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

      req.user = {
        id: rows[0].id,
        username: rows[0].username,
        discord_id: discordId
      };
      req.source = "discord";
      return next();
    }


    // 🟡 3. Web (JWT)
    if (origin === "web") {

      if (!auth.startsWith("Bearer ")) {
        return res.status(403).json({ error: "invalid_auth_scheme_for_web" });
      }

      const token = auth.slice(7).trim();
      const payload = jwt.verify(token, JWT_SECRET);

      req.user = {
        id: payload.sub,
        username: payload.username
      };
      req.source = "web";
      return next();
    }

    // ❌ Unbekannt
    return res.status(400).json({ error: "unknown_origin" });

  } catch (err) {
    console.error(err);
    return res.status(401).json({ error: "invalid_or_expired_token" });
  }
}
