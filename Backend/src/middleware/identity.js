import jwt from "jsonwebtoken";
import pool from "../services/mysql.service.js";

/**
 * Akzeptiert:
 * 1) Authorization: Bearer <JWT>
 *    -> JWT wird mit JWT_SECRET geprueft, nimmt z.B. { sub: <authme.username> } oder { uid: <authme.id> }
 * 2) Authorization: Bot <BOT_API_KEY> + X-Discord-User: <snowflake>
 *    -> discord_id Lookup in authme, liefert authme-User
 *
 * Setzt bei Erfolg: req.user = { id, username, discord_id }
 */
export default async function identity(req, res, next) {
  try {
    const auth = req.header("authorization") || "";
    if (!auth) return res.status(401).json({ error: "unauthenticated" });

    // Variante 1: Bearer JWT
    if (auth.toLowerCase().startsWith("bearer ")) {
      const token = auth.slice(7).trim();
      let payload;
      try {
        payload = jwt.verify(token, process.env.JWT_SECRET);
      } catch {
        return res.status(401).json({ error: "invalid_jwt" });
      }

      // payload kann username ODER uid enthalten
      if (payload.uid) {
        const [rows] = await pool.query(
          "SELECT id, username, uuid, ? AS discord_id FROM authme WHERE id = ?",
          [null, payload.uid]
        );
        if (!rows.length) return res.status(401).json({ error: "user_not_found" });
        req.user = { id: rows[0].id, username: rows[0].username, discord_id: null };
        return next();
      }

      if (payload.sub) {
        const [rows] = await pool.query(
          "SELECT id, username, uuid, discord_id FROM authme WHERE username = ?",
          [payload.sub]
        );
        if (!rows.length) return res.status(401).json({ error: "user_not_found" });
        const u = rows[0];
        req.user = { id: u.id, username: u.username, discord_id: u.discord_id || null };
        return next();
      }

      return res.status(400).json({ error: "jwt_missing_uid_or_sub" });
    }

    // Variante 2: Bot + Discord
    if (auth.toLowerCase().startsWith("bot ")) {
      const key = auth.slice(4).trim();
      if (key !== process.env.BOT_API_KEY) return res.status(401).json({ error: "invalid_bot_key" });
      const discordUser = req.header("x-discord-user");
      if (!discordUser) return res.status(400).json({ error: "missing_x_discord_user" });

      const [rows] = await pool.query(
        "SELECT id, username, uuid, discord_id FROM authme WHERE discord_id = ?",
        [discordUser]
      );
      if (!rows.length) return res.status(404).json({ error: "discord_not_linked" });

      const u = rows[0];
      req.user = { id: u.id, username: u.username, discord_id: u.discord_id || null };
      return next();
    }

    return res.status(400).json({ error: "unsupported_auth_scheme" });
  } catch (err) {
    next(err);
  }
}
