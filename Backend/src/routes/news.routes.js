import { Router } from "express";
import pool from "../services/mysql.service.js";

const r = Router();

const BOT_IP = process.env.DISCORD_BOT_IP || "127.0.0.1";
const BOT_PORT = process.env.DISCORD_BOT_PORT || "8081";
const BOT_BASE = `http://${BOT_IP}:${BOT_PORT}`;

/**
 * POST /news
 * Neue News erstellen
 * Body: { title, content, author, image_url? }
 */
r.post("/", async (req, res, next) => {
  try {
    const { title, content, author, image_url } = req.body;

    if (!title || !content || !author) {
      return res.status(400).json({ error: "title, content und author sind Pflichtfelder" });
    }

    const [result] = await pool.query(
      `INSERT INTO news_posts (title, content, author, image_url)
       VALUES (?, ?, ?, ?)`,
      [title, content, author, image_url || null]
    );

    const insertedId = result.insertId;

    let messageId = null;
    try {
      if (typeof fetch === "undefined") {
        // node <18: attempt dynamic import of node-fetch
        const nf = await import("node-fetch");
        // @ts-ignore
        globalThis.fetch = nf.default;
      }

      const resp = await fetch(`${BOT_BASE}/news-create`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title, content, author, image_url })
      });

      if (resp.ok) {
        const j = await resp.json();
        messageId = j?.message_id ?? null;
        if (messageId) {
          await pool.query(
            "UPDATE news_posts SET discord_message_id = ? WHERE id = ?",
            [messageId, insertedId]
          );
        }
      }
    } catch (err) {
      console.error("[news.routes] error posting to bot webhook:", err);
    }

    // Always fetch the row after updating discord_message_id
    const [rows] = await pool.query(
      "SELECT * FROM news_posts WHERE id = ?",
      [insertedId]
    );

    return res.status(201).json(rows[0]);
  } catch (err) {
    next(err);
  }
});

/**
 * GET /news
 * Alle News abrufen (optional mit limit/offset)
 */
r.get("/", async (req, res, next) => {
  try {
    const limit = parseInt(req.query.limit, 10) || 50;
    const offset = parseInt(req.query.offset, 10) || 0;

    const [rows] = await pool.query(
      `SELECT id, title, content, author, image_url, discord_message_id, created_at, updated_at
       FROM news_posts
       ORDER BY created_at DESC
       LIMIT ? OFFSET ?`,
      [limit, offset]
    );

    return res.json({
      news_posts: rows
    });
  } catch (err) {
    next(err);
  }
});

/**
 * GET /news/:id
 * Eine einzelne News holen
 */
r.get("/:id", async (req, res, next) => {
  try {
    const id = parseInt(req.params.id, 10);

    const [rows] = await pool.query(
      "SELECT * FROM news_posts WHERE id = ?",
      [id]
    );

    if (rows.length === 0) {
      return res.status(404).json({ error: "News nicht gefunden" });
    }

    return res.json(rows[0]);
  } catch (err) {
    next(err);
  }
});

/**
 * PUT /news/:id
 * News komplett updaten (title, content, author, image_url)
 */
r.put("/:id", async (req, res, next) => {
  try {
    const id = parseInt(req.params.id, 10);
    const { title, content, author, image_url } = req.body;

    if (!title || !content || !author) {
      return res.status(400).json({ error: "title, content und author sind Pflichtfelder" });
    }

    const [result] = await pool.query(
      `UPDATE news_posts
       SET title = ?, content = ?, author = ?, image_url = ?
       WHERE id = ?`,
      [title, content, author, image_url || null, id]
    );

    if (result.affectedRows === 0) {
      return res.status(404).json({ error: "News nicht gefunden" });
    }

    const [rows] = await pool.query(
      "SELECT * FROM news_posts WHERE id = ?",
      [id]
    );

    return res.json(rows[0]);
  } catch (err) {
    next(err);
  }
});

/**
 * DELETE /news/:id
 * News löschen
 */
r.delete("/:id", async (req, res, next) => {
  try {
    const id = parseInt(req.params.id, 10);

    // Fetch the row to see if there's a discord_message_id to delete in Discord
    const [rows] = await pool.query(
      "SELECT discord_message_id FROM news_posts WHERE id = ?",
      [id]
    );

    if (rows.length === 0) {
      return res.status(404).json({ error: "News nicht gefunden" });
    }

    const discordMessageId = rows[0].discord_message_id;

    if (discordMessageId) {
      try {
        // Log outgoing message_id and type
        console.log(`[news.routes] Deleting Discord message_id:`, discordMessageId, `type:`, typeof discordMessageId);
        if (typeof fetch === "undefined") {
          const nf = await import("node-fetch");
          // @ts-ignore
          globalThis.fetch = nf.default;
        }

        // Always send as stringified integer
        await fetch(`${BOT_BASE}/news-delete`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ message_id: String(discordMessageId) })
        });
      } catch (err) {
        console.error("[news.routes] error deleting message in bot webhook:", err);
      }
    }

    const [delRes] = await pool.query(
      "DELETE FROM news_posts WHERE id = ?",
      [id]
    );

    if (delRes.affectedRows === 0) {
      return res.status(404).json({ error: "News nicht gefunden" });
    }

    return res.status(204).send();
  } catch (err) {
    next(err);
  }
});

export default r;
