// src/routes/initiatives.routes.js
import { Router } from "express";
import pool from "../services/mysql.service.js";
import { getCurrentPhase } from "../utils/phases.util.js";

const r = Router();

function phaseToFinalVoteTable(phase) {
  // Phase 1 = Admin-Voting, Phase 2/3 = Spieler-Voting
  if (phase === 1) return "admin_votes";
  return "final_votes";
}

async function getFinalVoteCounts(initiativeId) {
  const phase = await getCurrentPhase();
  const table = phaseToFinalVoteTable(phase);

  const [[finals]] = await pool.query(
    `SELECT
       SUM(stimme = 1) AS ja,
       SUM(stimme = 0) AS nein
     FROM ${table}
     WHERE initiative_id = ?`,
    [initiativeId]
  );

  return {
    phase,
    table,
    ja: finals?.ja || 0,
    nein: finals?.nein || 0,
  };
}

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

      const finals = await getFinalVoteCounts(ini.id);

      ini.stimmen = {
        normal: normalVotes,
        ja: finals.ja,
        nein: finals.nein,
        // optional debug:
        // phase: finals.phase,
        // table: finals.table,
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

    if (rows.length === 0) return res.status(404).json({ error: "not_found" });

    const initiative = rows[0];

    const [[{ count: normalVotes }]] = await pool.query(
      "SELECT COUNT(*) AS count FROM votes WHERE initiative_id = ?",
      [initiative.id]
    );

    const finals = await getFinalVoteCounts(initiative.id);

    initiative.stimmen = {
      normal: normalVotes,
      ja: finals.ja,
      nein: finals.nein,
    };

    res.json(initiative);
  } catch (err) {
    next(err);
  }
});

r.post("/create", async (req, res, next) => {
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

    if (exists.length > 0) return res.status(409).json({ error: "title_already_exists" });

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

r.put("/edit/:id", async (req, res, next) => {
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

    if (rows.length === 0) return res.status(404).json({ error: "not_found" });
    if (!rows[0].aktiv) return res.status(400).json({ error: "initiative_not_active" });
    if (rows[0].author_id !== userId) return res.status(403).json({ error: "not_author" });

    await pool.query(
      "UPDATE initiatives SET title=?, description=? WHERE id=?",
      [title, description, req.params.id]
    );

    res.json({ id: req.params.id, title, description });
  } catch (err) {
    next(err);
  }
});

r.delete("/del/:id", async (req, res, next) => {
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

    if (rows.length === 0) return res.status(404).json({ error: "not_found" });
    if (!rows[0].aktiv) return res.status(400).json({ error: "initiative_not_active" });
    if (rows[0].author_id !== userId) return res.status(403).json({ error: "not_author" });

    await pool.query("DELETE FROM initiatives WHERE id=?", [req.params.id]);

    res.json({ id: req.params.id, deleted: true });
  } catch (err) {
    next(err);
  }
});

r.post("/vote/:id", async (req, res, next) => {
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

    if (rows.length === 0) return res.status(404).json({ error: "initiative_not_found" });
    if (!rows[0].aktiv) return res.status(400).json({ error: "initiative_not_active" });

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

      const finals = await getFinalVoteCounts(ini.id);

      result.push({
        ...ini,
        stimmen: {
          normal,
          ja: finals.ja,
          nein: finals.nein,
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
    voteType: typeof vote,
  });

  if (!Number.isInteger(initiativeId) || initiativeId <= 0) {
    console.warn("[FINALVOTE] invalid initiative id", { initiativeId });
    return res.status(400).json({ error: "invalid_initiative_id" });
  }

  if (typeof vote !== "boolean") {
    console.warn("[FINALVOTE] invalid vote type", { userId, initiativeId, vote });
    return res.status(400).json({ error: "invalid_vote_must_be_boolean" });
  }

  const stimme = vote ? 1 : 0;

  try {
    const phase = await getCurrentPhase();
    console.log("[FINALVOTE] current phase", { initiativeId, phase });

    if (phase === 0 || phase === 3) {
      console.warn("[FINALVOTE] vote not allowed in phase", { userId, initiativeId, phase });
      return res.status(400).json({
        error: "finalvote_not_allowed_in_this_phase",
        phase,
      });
    }

    if (phase === 1 && !req.user.isAdmin) {
      console.warn("[FINALVOTE] non-admin tried finalvote in phase 1", { userId, initiativeId });
      return res.status(403).json({ error: "only_admin_can_finalvote_in_phase_1" });
    }

    const table = phase === 1 ? "admin_votes" : "final_votes";

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
      `SELECT id, stimme FROM ${table} WHERE initiative_id = ? AND user_id = ?`,
      [initiativeId, userId]
    );

    let action;

    if (existing.length === 0) {
      await pool.query(
        `INSERT INTO ${table} (initiative_id, user_id, stimme) VALUES (?, ?, ?)`,
        [initiativeId, userId, stimme]
      );
      action = "created";

      console.log("[FINALVOTE] vote created", { table, userId, initiativeId, stimme });
    } else {
      await pool.query(
        `UPDATE ${table} SET stimme = ? WHERE id = ?`,
        [stimme, existing[0].id]
      );
      action = "updated";

      console.log("[FINALVOTE] vote updated", {
        table,
        userId,
        initiativeId,
        oldStimme: existing[0].stimme,
        newStimme: stimme,
      });
    }

    console.log("[FINALVOTE] success", { table, userId, initiativeId, action, stimme });

    return res.json({
      id: initiativeId,
      final_voted: true,
      action,
      phase,
      vote,
    });
  } catch (err) {
    console.error("[FINALVOTE] error", { initiativeId, userId, error: err.message });
    next(err);
  }
});
// GET /initiatives/:id/finalvotes
// Liefert die Finalvote-Zusammenfassung (ja/nein) + optional die Einzelstimmen (admin_votes oder final_votes je nach Phase)

r.get("/:id/finalvotes", async (req, res, next) => {
  const initiativeId = Number(req.params.id);

  if (!Number.isInteger(initiativeId) || initiativeId <= 0) {
    return res.status(400).json({ error: "invalid_initiative_id" });
  }

  try {
    // sicherstellen, dass Initiative existiert + aktiv ist (wie bei deinen anderen Routen)
    const [rows] = await pool.query(
      "SELECT id, aktiv FROM initiatives WHERE id = ?",
      [initiativeId]
    );

    if (rows.length === 0) return res.status(404).json({ error: "initiative_not_found" });
    if (!rows[0].aktiv) return res.status(400).json({ error: "initiative_not_active" });

    // Phase bestimmen → richtige Tabelle
    const phase = await getCurrentPhase();
    const table = phaseToFinalVoteTable(phase); // 1 => admin_votes, sonst final_votes

    // Counts
    const [[finals]] = await pool.query(
      `SELECT
         SUM(stimme = 1) AS ja,
         SUM(stimme = 0) AS nein
       FROM ${table}
       WHERE initiative_id = ?`,
      [initiativeId]
    );

    // Optional: Liste der Stimmen (damit Frontend nicht pro Initiative mehrfach raten muss)
    const [votes] = await pool.query(
      `
      SELECT
        v.id AS vote_id,
        v.user_id,
        a.username,
        v.stimme,
        v.created_at,
        v.updated_at
      FROM ${table} v
      JOIN authme a ON a.id = v.user_id
      WHERE v.initiative_id = ?
      ORDER BY v.created_at ASC
      `,
      [initiativeId]
    );

    return res.json({
      initiative_id: initiativeId,
      phase,
      table,
      ja: finals?.ja || 0,
      nein: finals?.nein || 0,
      votes: votes.map((v) => ({
        vote_id: v.vote_id,
        user_id: v.user_id,
        username: v.username,
        vote: v.stimme === 1,   // boolean fürs Frontend
        stimme: v.stimme,       // int debug/legacy
        created_at: v.created_at,
        updated_at: v.updated_at,
      })),
    });
  } catch (err) {
    next(err);
  }
});

export default r;
