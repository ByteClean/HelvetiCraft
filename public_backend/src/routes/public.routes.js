import { Router } from "express";
import pool from "../services/mysql.service.js";

const r = Router();

// health
r.get("/health", (req, res) => res.json({ ok: true }));

// ---- initiatives (public read) ----
r.get(["/initiatives", "/initiatives/all"], async (req, res, next) => {
  try {
    const [initiatives] = await pool.query(`
      SELECT 
        i.id, i.title, i.description, i.status, i.aktiv,
        i.created_at, i.updated_at,
        u.username AS author
      FROM initiatives i
      JOIN authme u ON u.id = i.author_id
      WHERE i.aktiv = 1
      ORDER BY i.id DESC
    `);

    for (const ini of initiatives) {
      const [[{ count: normalVotes }]] = await pool.query(
        "SELECT COUNT(*) AS count FROM votes WHERE initiative_id = ?",
        [ini.id]
      );

      const [[finals]] = await pool.query(
        `SELECT 
           SUM(stimme = 1) AS ja,
           SUM(stimme = 0) AS nein
         FROM final_votes WHERE initiative_id = ?`,
        [ini.id]
      );

      ini.stimmen = {
        normal: normalVotes,
        ja: finals.ja || 0,
        nein: finals.nein || 0,
      };
    }

    res.json(initiatives);
  } catch (err) {
    next(err);
  }
});

r.get("/initiatives/leaderboard", async (req, res, next) => {
  try {
    const [initiatives] = await pool.query(`
      SELECT i.id, i.title, i.description, i.status, i.aktiv,
             i.created_at,
             u.username AS author
      FROM initiatives i
      JOIN authme u ON u.id = i.author_id
      WHERE i.aktiv = 1
      ORDER BY i.created_at ASC
    `);

    const result = [];
    for (const ini of initiatives) {
      const [[{ count: normal }]] = await pool.query(
        "SELECT COUNT(*) AS count FROM votes WHERE initiative_id = ?",
        [ini.id]
      );

      const [[finals]] = await pool.query(
        `SELECT 
           SUM(stimme = 1) AS ja,
           SUM(stimme = 0) AS nein
         FROM final_votes WHERE initiative_id = ?`,
        [ini.id]
      );

      result.push({
        ...ini,
        stimmen: {
          normal,
          ja: finals.ja || 0,
          nein: finals.nein || 0,
        },
      });
    }

    res.json(result);
  } catch (err) {
    next(err);
  }
});

r.get("/initiatives/:id", async (req, res, next) => {
  try {
    const [rows] = await pool.query(
      `
      SELECT i.id, i.title, i.description, i.status, i.aktiv,
             i.created_at, i.updated_at,
             u.username AS author
      FROM initiatives i
      JOIN authme u ON u.id = i.author_id
      WHERE i.id = ? AND i.aktiv = 1
      `,
      [req.params.id]
    );

    if (rows.length === 0) return res.status(404).json({ error: "not_found" });

    const initiative = rows[0];

    const [[{ count: normalVotes }]] = await pool.query(
      "SELECT COUNT(*) AS count FROM votes WHERE initiative_id = ?",
      [initiative.id]
    );

    const [[finals]] = await pool.query(
      `SELECT 
         SUM(stimme = 1) AS ja,
         SUM(stimme = 0) AS nein
       FROM final_votes WHERE initiative_id = ?`,
      [initiative.id]
    );

    initiative.stimmen = {
      normal: normalVotes,
      ja: finals.ja || 0,
      nein: finals.nein || 0,
    };

    res.json(initiative);
  } catch (err) {
    next(err);
  }
});

r.get("/initiatives/:id/votes", async (req, res, next) => {
  const initiativeId = Number(req.params.id);
  if (!Number.isInteger(initiativeId)) {
    return res.status(400).json({ error: "invalid_initiative_id" });
  }

  try {
    const [[{ count }]] = await pool.query(
      "SELECT COUNT(*) AS count FROM votes WHERE initiative_id = ?",
      [initiativeId]
    );

    const [votes] = await pool.query(
      `SELECT v.id AS vote_id, v.created_at,
              a.id AS user_id, a.username
       FROM votes v
       JOIN authme a ON a.id = v.user_id
       WHERE v.initiative_id = ?
       ORDER BY v.created_at ASC`,
      [initiativeId]
    );

    res.json({
      initiative_id: initiativeId,
      normal_votes: count,
      votes,
    });
  } catch (err) {
    next(err);
  }
});

// ---- phases/current (public read) ----
r.get("/phases/current", async (req, res, next) => {
  try {
    const [[row]] = await pool.query(`
      SELECT phase, start_phase0, start_phase1, start_phase2, start_phase3,
             duration_phase0, duration_phase1, duration_phase2, duration_phase3,
             aktiv
      FROM phases
      WHERE aktiv = 1
      ORDER BY id DESC
      LIMIT 1
    `);

    if (!row) return res.status(404).json({ error: "no_active_phase_cycle" });

    res.json({
      phase: row.phase,
      start_phase0: row.start_phase0,
      start_phase1: row.start_phase1,
      start_phase2: row.start_phase2,
      start_phase3: row.start_phase3,
      duration_phase0: row.duration_phase0,
      duration_phase1: row.duration_phase1,
      duration_phase2: row.duration_phase2,
      duration_phase3: row.duration_phase3,
      aktiv: row.aktiv,
    });
  } catch (err) {
    next(err);
  }
});

// ---- news (public read) ----
r.get("/news", async (req, res, next) => {
  try {
    const [rows] = await pool.query(
      "SELECT * FROM news ORDER BY id DESC"
    );
    res.json(rows);
  } catch (err) {
    next(err);
  }
});

r.get("/news/:id", async (req, res, next) => {
  try {
    const [rows] = await pool.query("SELECT * FROM news WHERE id = ? LIMIT 1", [
      req.params.id,
    ]);
    if (rows.length === 0) return res.status(404).json({ error: "not_found" });
    res.json(rows[0]);
  } catch (err) {
    next(err);
  }
});

// ---- quiz: ranking erlaubt, question NICHT ----
r.get("/quiz/ranking", async (req, res, next) => {
  try {
    const [rows] = await pool.query(
      "SELECT * FROM quiz_ranking ORDER BY score DESC LIMIT 100"
    );
    res.json(rows);
  } catch (err) {
    next(err);
  }
});

r.get("/quiz/question", (req, res) => {
  res.status(404).json({ error: "not_found" });
});

export default r;
