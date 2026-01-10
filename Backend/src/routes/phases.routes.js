// src/routes/phases.routes.js
import { Router } from "express";
import pool from "../services/mysql.service.js";
import { verifyAuth } from "../middleware/auth.middleware.js";
import {
  getMinVotes,
  advancePhaseAndEvaluate,
  startPhases,
} from "../utils/phases.util.js";

const r = Router();

/**
 * GET /phases/current
 * Liefert aktive Runde: Phase + Startzeiten + Schwelle.
 */
r.get("/current", async (req, res, next) => {
  try {
    const [[row]] = await pool.query(`
      SELECT
        id, phase, aktiv,
        start_phase0, start_phase1, start_phase2, start_phase3,
        duration_phase0, duration_phase1, duration_phase2, duration_phase3
      FROM phases
      WHERE aktiv = 1
      ORDER BY id DESC
      LIMIT 1
    `);

    if (!row) {
      return res.status(404).json({ error: "no_active_phase_cycle" });
    }

    const { activePlayers, minVotes } = await getMinVotes(10);

    res.json({
      cycleId: row.id,
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
      activePlayers,
      minVotes,
    });
  } catch (err) {
    next(err);
  }
});

/**
 * POST /phases/advance
 * Admin-only: wechselt aktive Phase + bewertet Initiativen.
 */
r.post("/advance", verifyAuth, async (req, res, next) => {
  if (!req.user.isAdmin) {
    return res.status(403).json({ error: "only_admin_can_advance_phase" });
  }

  try {
    const result = await advancePhaseAndEvaluate(10);
    res.json(result);
  } catch (err) {
    next(err);
  }
});

/**
 * POST /phases/start
 * Admin-only: startet sofort eine neue Runde (deaktiviert alte, erstellt neue).
 */
r.post("/start", verifyAuth, async (req, res, next) => {
  if (!req.user.isAdmin) {
    return res.status(403).json({ error: "only_admin_can_start_phases" });
  }

  try {
    const result = await startPhases();
    res.json(result);
  } catch (err) {
    next(err);
  }
});

export default r;
