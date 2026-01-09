import { Router } from "express"; 
import pool from "../services/mysql.service.js";
import { verifyAuth } from "../middleware/auth.middleware.js";
import { getCurrentPhase } from "../utils/phases.util.js";

const r = Router();


r.get(["/", "/all"], async (req, res, next) => {
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

r.get("/:id", async (req, res, next) => {
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

    if (rows.length === 0)
      return res.status(404).json({ error: "not_found" });

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


r.post("/create", verifyAuth, async (req, res, next) => {
  const { title, description } = req.body;
  const { id: author_id, username } = req.user;

  try {
    const phase = await getCurrentPhase();
    if (phase !== 0) {
      return res.status(400).json({
        error: "cannot_create_in_this_phase",
        phase,
      });
    }

    const [exists] = await pool.query(
      "SELECT id FROM initiatives WHERE title = ? LIMIT 1",
      [title]
    );

    if (exists.length > 0)
      return res.status(409).json({ error: "title_already_exists" });

    const [result] = await pool.query(
      "INSERT INTO initiatives (author_id, title, description, status, aktiv) VALUES (?, ?, ?, 0, 1)",
      [author_id, title, description]
    );

    res.status(201).json({
      id: result.insertId,
      author_id,
      username,
      title,
      description,
      status: 0,
      aktiv: 1,
    });
  } catch (err) {
    next(err);
  }
});


r.put("/edit/:id", verifyAuth, async (req, res, next) => {
  const { title, description } = req.body;
  const userId = req.user.id;

  try {
    const phase = await getCurrentPhase();
    if (phase !== 0) {
      return res.status(400).json({
        error: "cannot_edit_in_this_phase",
        phase,
      });
    }

    const [rows] = await pool.query(
      "SELECT author_id, aktiv FROM initiatives WHERE id=?",
      [req.params.id]
    );

    if (rows.length === 0)
      return res.status(404).json({ error: "not_found" });

    if (!rows[0].aktiv)
      return res.status(400).json({ error: "initiative_not_active" });

    if (rows[0].author_id !== userId)
      return res.status(403).json({ error: "not_author" });

    await pool.query(
      "UPDATE initiatives SET title=?, description=? WHERE id=?",
      [title, description, req.params.id]
    );

    res.json({ id: req.params.id, title, description });
  } catch (err) {
    next(err);
  }
});


r.delete("/del/:id", verifyAuth, async (req, res, next) => {
  const userId = req.user.id;

  try {
    const phase = await getCurrentPhase();
    if (phase !== 0) {
      return res.status(400).json({
        error: "cannot_delete_in_this_phase",
        phase,
      });
    }

    const [rows] = await pool.query(
      "SELECT author_id, aktiv FROM initiatives WHERE id=?",
      [req.params.id]
    );

    if (rows.length === 0)
      return res.status(404).json({ error: "not_found" });

    if (!rows[0].aktiv)
      return res.status(400).json({ error: "initiative_not_active" });

    if (rows[0].author_id !== userId)
      return res.status(403).json({ error: "not_author" });

    await pool.query("DELETE FROM initiatives WHERE id=?", [req.params.id]);

    res.json({ id: req.params.id, deleted: true });
  } catch (err) {
    next(err);
  }
});


r.post("/vote/:id", verifyAuth, async (req, res, next) => {
  const initiativeId = Number(req.params.id);
  const userId = req.user.id;

  if (!Number.isInteger(initiativeId) || initiativeId <= 0)
    return res.status(400).json({ error: "invalid_initiative_id" });

  try {
    const phase = await getCurrentPhase();
    if (phase !== 0) {
      return res.status(400).json({
        error: "normal_votes_only_allowed_in_phase_0",
        phase,
      });
    }

    const [rows] = await pool.query(
      "SELECT id, aktiv FROM initiatives WHERE id = ?",
      [initiativeId]
    );

    if (rows.length === 0)
      return res.status(404).json({ error: "initiative_not_found" });

    if (!rows[0].aktiv)
      return res.status(400).json({ error: "initiative_not_active" });

    
    const [existing] = await pool.query(
      "SELECT id FROM votes WHERE initiative_id = ? AND user_id = ? LIMIT 1",
      [initiativeId, userId]
    );

    if (existing.length > 0) {
      await pool.query(
        "DELETE FROM votes WHERE initiative_id = ? AND user_id = ?",
        [initiativeId, userId]
      );

      return res.json({
        id: initiativeId,
        voted: false,
        action: "removed",
      });
    }

    // FALL 2: Noch kein Vote → hinzufügen
    await pool.query(
      "INSERT INTO votes (initiative_id, user_id) VALUES (?, ?)",
      [initiativeId, userId]
    );

    return res.json({
      id: initiativeId,
      voted: true,
      action: "added",
    });
  } catch (err) {
    next(err);
  }
});



r.get("/:id/votes", async (req, res, next) => {
  const initiativeId = Number(req.params.id);

  if (!Number.isInteger(initiativeId))
    return res.status(400).json({ error: "invalid_initiative_id" });

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

r.get("/leaderboard", async (req, res, next) => {
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


r.post("/finalvote/:id", async (req, res, next) => {
  const initiativeId = Number(req.params.id);
  const userId = req.user.id;
  const { vote } = req.body;

  console.log("[FINALVOTE] request", {
    initiativeId,
    userId,
    vote,
  });

  if (!Number.isInteger(initiativeId) || initiativeId <= 0) {
    console.warn("[FINALVOTE] invalid initiative id", { initiativeId });
    return res.status(400).json({ error: "invalid_initiative_id" });
  }

  if (vote !== 0 && vote !== 1) {
    console.warn("[FINALVOTE] invalid vote value", {
      userId,
      initiativeId,
      vote,
    });
    return res.status(400).json({ error: "invalid_vote_must_be_0_or_1" });
  }

  const stimme = vote;

  try {
    const phase = await getCurrentPhase();
    console.log("[FINALVOTE] current phase", {
      initiativeId,
      phase,
    });

    if (phase === 0 || phase === 3) {
      console.warn("[FINALVOTE] vote not allowed in phase", {
        userId,
        initiativeId,
        phase,
      });
      return res.status(400).json({
        error: "finalvote_not_allowed_in_this_phase",
        phase,
      });
    }

    if (phase === 1 && !req.user.isAdmin) {
      console.warn("[FINALVOTE] non-admin tried finalvote in phase 1", {
        userId,
        initiativeId,
      });
      return res.status(403).json({
        error: "only_admin_can_finalvote_in_phase_1",
      });
    }

    const [rows] = await pool.query(
      "SELECT id, aktiv FROM initiatives WHERE id = ?",
      [initiativeId]
    );

    if (rows.length === 0) {
      console.warn("[FINALVOTE] initiative not found", { initiativeId });
      return res.status(404).json({ error: "initiative_not_found" });
    }

    if (!rows[0].aktiv) {
      console.warn("[FINALVOTE] initiative not active", { initiativeId });
      return res.status(400).json({ error: "initiative_not_active" });
    }

    const [existing] = await pool.query(
      "SELECT id, stimme FROM final_votes WHERE initiative_id = ? AND user_id = ?",
      [initiativeId, userId]
    );

    let action;

    if (existing.length === 0) {
      await pool.query(
        "INSERT INTO final_votes (initiative_id, user_id, stimme) VALUES (?, ?, ?)",
        [initiativeId, userId, stimme]
      );
      action = "created";

      console.log("[FINALVOTE] vote created", {
        userId,
        initiativeId,
        stimme,
      });
    } else {
      await pool.query(
        "UPDATE final_votes SET stimme = ? WHERE id = ?",
        [stimme, existing[0].id]
      );
      action = "updated";

      console.log("[FINALVOTE] vote updated", {
        userId,
        initiativeId,
        oldStimme: existing[0].stimme,
        newStimme: stimme,
      });
    }

    console.log("[FINALVOTE] success", {
      userId,
      initiativeId,
      action,
      stimme,
    });

    return res.json({
      id: initiativeId,
      final_voted: true,
      action,
      vote: stimme,
    });
  } catch (err) {
    console.error("[FINALVOTE] error", {
      initiativeId,
      userId,
      error: err.message,
    });
    next(err);
  }
});



export default r;
