// src/routes/phases.routes.js
import { Router } from "express";
import pool from "../services/mysql.service.js";
import { verifyAuth } from "../middleware/auth.middleware.js";
import {
  getCurrentPhase,
  getMinVotes,
  advancePhaseAndEvaluate,
  startPhases
} from "../utils/phases.util.js";

const r = Router();

/**
 * GET /phases/current
 * Liefert aktuelle Phase + Startzeiten + Schwelle.
 */
r.get("/current", async (req, res, next) => {
  try {
    const [[row]] = await pool.query(
      "SELECT phase, start_phase0, start_phase1, start_phase2, start_phase3 FROM phases WHERE id = 1"
    );
    if (!row) {
      return res.status(500).json({ error: "phases_row_missing" });
    }

    const { activePlayers, minVotes } = await getMinVotes(10);

    res.json({
      phase: row.phase, // 0–3
      start_phase0: row.start_phase0,
      start_phase1: row.start_phase1,
      start_phase2: row.start_phase2,
      start_phase3: row.start_phase3,
      activePlayers,
      minVotes,
    });
  } catch (err) {
    next(err);
  }
});

/**
 * POST /phases/advance
 * Admin-only: wechselt globale Phase + bewertet Initiativen.
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


r.post("/start", verifyAuth, async (req, res, next) => {
  if (!req.user.isAdmin) {
    return res.status(403).json({ error: "only_admin_can_start_phases" });
  }

  try {
    const result = await startPhases();
    return res.json(result);
  } catch (err) {
    next(err);
  }
});

export default r;
