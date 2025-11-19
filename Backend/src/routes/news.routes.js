import { Router } from "express";
import pool from "../services/mysql.service.js";

const r = Router();

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
      `SELECT id, title, author, image_url, created_at, updated_at
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

    const [result] = await pool.query(
      "DELETE FROM news_posts WHERE id = ?",
      [id]
    );

    if (result.affectedRows === 0) {
      return res.status(404).json({ error: "News nicht gefunden" });
    }

    return res.status(204).send(); // kein Content, nur Erfolg
  } catch (err) {
    next(err);
  }
});

export default r;
