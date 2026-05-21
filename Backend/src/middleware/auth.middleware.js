// src/middleware/auth.middleware.js

import jwt from "jsonwebtoken";
import pool from "../services/mysql.service.js";

const JWT_SECRET = process.env.JWT_SECRET;
const MINECRAFT_API_KEY = process.env.MINECRAFT_API_KEY;
const DISCORD_API_KEY = process.env.BOT_API_KEY;
const WEBSITE_API_KEY = process.env.WEBSITE_API_KEY;

/**
 * Einheitliche Authentifizierung:
 * - Web (JWT)
 * - Minecraft (API Key + UUID)
 * - Discord Bot (API Key + Discord ID)
 * - Website Service (API Key)
 */
export async function verifyAuth(req, res, next) {
  try {

    // -----------------------------------------------------------
    // 0) WEB LOGIN (JWT) - MUSS ZUERST KOMMEN
    // -----------------------------------------------------------
    const authHeader = req.headers.authorization;

    if (authHeader?.startsWith("Bearer ")) {
      try {
        const token = authHeader.split(" ")[1];

        const payload = jwt.verify(token, JWT_SECRET);

        const [rows] = await pool.query(
          "SELECT id, username, discord_id, isAdmin FROM authme WHERE id = ? LIMIT 1",
          [payload.sub]
        );

        if (rows.length === 0) {
          return res.status(401).json({ error: "jwt_user_not_found" });
        }

        const u = rows[0];

        req.user = {
          id: u.id,
          username: u.username,
          discord_id: u.discord_id || null,
          isAdmin: u.isAdmin === 1
        };

        req.source = "web";
        return next();

      } catch (err) {
        console.error("JWT error:", err);
        return res.status(401).json({ error: "invalid_token" });
      }
    }

    // -----------------------------------------------------------
    // 1) REST (Minecraft / Discord / Website)
    // -----------------------------------------------------------

    const origin = req.headers["x-auth-from"];
    const key = req.headers["x-auth-key"];

    if (!origin) {
      return res.status(400).json({ error: "missing_x-auth-from_header" });
    }

    if (!key) {
      return res.status(401).json({ error: "missing_x-auth-key_header" });
    }

    // -----------------------------------------------------------
    // 2) MINECRAFT
    // -----------------------------------------------------------
    if (origin === "minecraft") {

      if (key !== MINECRAFT_API_KEY) {
        return res.status(403).json({ error: "invalid_minecraft_key" });
      }

      const uuid = req.headers["x-uuid"];

      if (!uuid) {
        return res.status(400).json({ error: "missing_x-uuid_header" });
      }

      if (uuid === "00000000-0000-0000-0000-000000000000") {
        req.user = {
          id: null,
          username: "government",
          discord_id: null,
          isAdmin: true
        };
        req.source = "minecraft";
        return next();
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
    // 3) DISCORD
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

    // -----------------------------------------------------------
    // 4) WEBSITE API KEY (SERVER-TO-SERVER)
    // -----------------------------------------------------------
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
    // UNKNOWN ORIGIN
    // -----------------------------------------------------------
    return res.status(400).json({ error: "unknown_origin" });

  } catch (err) {
    console.error("Auth Middleware Error:", err);
    return res.status(500).json({ error: "internal_auth_error" });
  }
}