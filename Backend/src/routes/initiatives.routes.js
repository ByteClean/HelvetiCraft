import { Router } from "express";
import pool from "../services/mysql.service.js";

const r = Router();

/**
 * Alle Initiativen
 */
r.get("/all", async (req, res, next) => {
  try {
    const [rows] = await pool.query(
      `SELECT i.id, i.title, i.description, i.status,
              i.created_at, i.updated_at,
              u.username AS author
       FROM initiatives i
       JOIN authme u ON u.id = i.author_id`
    );
    res.json(rows);
  } catch (err) {
    next(err);
  }
});

/**
 * Einzelne Initiative
 */
r.get("/:id", async (req, res, next) => {
  try {
    const [rows] = await pool.query(
      `SELECT i.id, i.title, i.description, i.status,
              i.created_at, i.updated_at,
              u.username AS author
       FROM initiatives i
       JOIN authme u ON u.id = i.author_id
       WHERE i.id = ?`,
      [req.params.id]
    );
    if (rows.length === 0) return res.status(404).json({ error: "not_found" });
    res.json(rows[0]);
  } catch (err) {
    next(err);
  }
});
/**
 * Neue Initiative anlegen
 * Body: { author_id, title, description }
 */
r.post("/new", async (req, res, next) => {
  const { author_id, title, description } = req.body;
  try {
    const [result] = await pool.query(
      "INSERT INTO initiatives (author_id, title, description) VALUES (?,?,?)",
      [author_id, title, description]
    );
    res.status(201).json({ id: result.insertId, author_id, title, description, status: "draft" });
  } catch (err) {
    if (err.code === "ER_NO_REFERENCED_ROW_2") {
      return res.status(400).json({ error: "invalid_author_id" });
    }
    next(err);
  }
});

/**
 * Initiative bearbeiten
 */
r.put("/edit/:id", async (req, res, next) => {
  const { title, description, status } = req.body;
  try {
    const [result] = await pool.query(
      "UPDATE initiatives SET title=?, description=?, status=? WHERE id=?",
      [title, description, status, req.params.id]
    );
    if (result.affectedRows === 0) return res.status(404).json({ error: "not_found" });
    res.json({ id: req.params.id, title, description, status });
  } catch (err) {
    next(err);
  }
});

/**
 * Initiative löschen
 */
r.delete("/del/:id", async (req, res, next) => {
  try {
    const [result] = await pool.query("DELETE FROM initiatives WHERE id=?", [req.params.id]);
    if (result.affectedRows === 0) return res.status(404).json({ error: "not_found" });
    res.json({ id: req.params.id, deleted: true });
  } catch (err) {
    next(err);
  }
});

export default r;
