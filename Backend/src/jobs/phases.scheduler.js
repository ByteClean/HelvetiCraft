// src/jobs/phases.scheduler.js
import pool from "../services/mysql.service.js";
import { advancePhaseAndEvaluate, endCycleAndRestart } from "../utils/phases.util.js";

function addDays(date, days) {
  return new Date(date.getTime() + days * 24 * 60 * 60 * 1000);
}

/**
 * intervalMs: wie oft prüfen
 * daysForMinVotes: Fenster für aktive Spieler (minVotes)
 *
 * Annahme: duration_phaseX sind TAGE.
 */
export function startPhaseScheduler({
  intervalMs = 12 * 60 * 60 * 1000,
  daysForMinVotes = 10,
} = {}) {
  setInterval(async () => {
    const conn = await pool.getConnection();
    try {
      // verhindert doppelte Ausführung bei mehreren Backend-Instanzen
      const [[lockRow]] = await conn.query(
        "SELECT GET_LOCK('phases_scheduler', 1) AS got"
      );
      if (!lockRow || lockRow.got !== 1) return;

      const [[row]] = await conn.query(`
        SELECT
          id, phase, aktiv,
          start_phase0, start_phase1, start_phase2, start_phase3,
          duration_phase0, duration_phase1, duration_phase2, duration_phase3
        FROM phases
        WHERE aktiv = 1
        ORDER BY id DESC
        LIMIT 1
      `);

      if (!row) return;

      const now = new Date();

      let start;
      let durationDays;

      if (row.phase === 0) { start = row.start_phase0; durationDays = row.duration_phase0; }
      else if (row.phase === 1) { start = row.start_phase1; durationDays = row.duration_phase1; }
      else if (row.phase === 2) { start = row.start_phase2; durationDays = row.duration_phase2; }
      else if (row.phase === 3) { start = row.start_phase3; durationDays = row.duration_phase3; }
      else return;

      if (!start) return;

      const end = addDays(new Date(start), Number(durationDays));
      if (now < end) return;

      if (row.phase < 3) {
        await advancePhaseAndEvaluate(daysForMinVotes);
      } else {
        // Phase 3 ist vorbei -> alte Runde deaktivieren + neue starten
        await endCycleAndRestart();
      }
    } catch (e) {
      console.error("[PHASE_SCHEDULER] error", e?.message || e);
    } finally {
      try {
        await conn.query("SELECT RELEASE_LOCK('phases_scheduler')");
      } catch {}
      conn.release();
    }
  }, intervalMs);
}
