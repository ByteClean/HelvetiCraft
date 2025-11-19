import { Router } from "express";
import pool from "../services/mysql.service.js";
import { requireAuth, identifySource } from "../middleware/auth.middleware.js";

const r = Router();

/**
 * Alle Initiativen (öffentlich)
 * GET /initiatives oder /initiatives/all
 */
r.get(["/", "/all"], async (req, res, next) => {
  try {
    const [rows] = await pool.query(`
      SELECT i.id, i.title, i.description, i.status,
             i.created_at, i.updated_at,
             u.username AS author
      FROM initiatives i
      JOIN authme u ON u.id = i.author_id
      ORDER BY i.id DESC
    `);
    res.json(rows);
  } catch (err) {
    next(err);
  }
});

/**
 * Einzelne Initiative (öffentlich)
 * GET /initiatives/:id
 */
r.get("/:id", async (req, res, next) => {
  try {
    const [rows] = await pool.query(`
      SELECT i.id, i.title, i.description, i.status,
             i.created_at, i.updated_at,
             u.username AS author
      FROM initiatives i
      JOIN authme u ON u.id = i.author_id
      WHERE i.id = ?
    `, [req.params.id]);
    if (rows.length === 0) return res.status(404).json({ error: "not_found" });
    res.json(rows[0]);
  } catch (err) {
    next(err);
  }
});

/**
 * Neue Initiative (Website / Minecraft / Discord)
 * POST /initiatives
 */
r.post("/create", identifySource, async (req, res, next) => {
  const { title, description } = req.body;
  const { id: author_id, username, source } = req.user;

  try {
    const [result] = await pool.query(
      "INSERT INTO initiatives (author_id, title, description) VALUES (?, ?, ?)",
      [author_id, title, description]
    );

    res.status(201).json({
      id: result.insertId,
      author_id,
      username,
      title,
      description,
      status: "draft",
      created_via: source
    });
  } catch (err) {
    if (err.code === "ER_NO_REFERENCED_ROW_2") {
      return res.status(400).json({ error: "invalid_author_id" });
    }
    next(err);
  }
});

/**
 * Initiative bearbeiten (nur eigene / authentifiziert)
 * PUT /initiatives/edit/:id
 */
r.put("/edit/:id", requireAuth, async (req, res, next) => {
  const { title, description, status } = req.body;
  const userId = req.user.sub;

  try {
    // nur eigene Initiative bearbeiten
    const [check] = await pool.query("SELECT author_id FROM initiatives WHERE id=?", [req.params.id]);
    if (check.length === 0) return res.status(404).json({ error: "not_found" });
    if (check[0].author_id !== userId)
      return res.status(403).json({ error: "not_author" });

    const [result] = await pool.query(
      "UPDATE initiatives SET title=?, description=?, status=? WHERE id=?",
      [title, description, status, req.params.id]
    );
    res.json({ id: req.params.id, title, description, status });
  } catch (err) {
    next(err);
  }
});

/**
 * Initiative löschen (nur eigene / authentifiziert)
 * DELETE /initiatives/del/:id
 */
r.delete("/del/:id", requireAuth, async (req, res, next) => {
  const userId = req.user.sub;
  try {
    const [check] = await pool.query("SELECT author_id FROM initiatives WHERE id=?", [req.params.id]);
    if (check.length === 0) return res.status(404).json({ error: "not_found" });
    if (check[0].author_id !== userId)
      return res.status(403).json({ error: "not_author" });

    const [result] = await pool.query("DELETE FROM initiatives WHERE id=?", [req.params.id]);
    res.json({ id: req.params.id, deleted: true });
  } catch (err) {
    next(err);
  }
});

export default r;
