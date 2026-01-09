import pool from "../services/mysql.service.js";
import { advancePhaseAndEvaluate, startPhases } from "../utils/phases.util.js";

function addDays(date, days) {
  return new Date(date.getTime() + days * 24 * 60 * 60 * 1000);
}

export async function startPhaseScheduler({ intervalMs = 60_000, daysForMinVotes = 10 } = {}) {
  setInterval(async () => {
    const conn = await pool.getConnection();
    try {
      // Schutz, falls du mehrere Backend-Instanzen hast (Docker/Scaling):
      const [[lockRow]] = await conn.query("SELECT GET_LOCK('phases_scheduler', 1) AS got");
      if (!lockRow || lockRow.got !== 1) return;

      const [[row]] = await conn.query(`
        SELECT
          phase, aktiv,
          start_phase0, start_phase1, start_phase2, start_phase3,
          duration_phase0, duration_phase1, duration_phase2, duration_phase3
        FROM phases
        WHERE id = 1
      `);

      if (!row || row.aktiv !== 1) return;

      const now = new Date();

      // Bestimme Endzeit der aktuellen Phase anhand start_phaseX + duration_phaseX
      let start;
      let durationDays;

      if (row.phase === 0) { start = row.start_phase0; durationDays = row.duration_phase0; }
      else if (row.phase === 1) { start = row.start_phase1; durationDays = row.duration_phase1; }
      else if (row.phase === 2) { start = row.start_phase2; durationDays = row.duration_phase2; }
      else if (row.phase === 3) { start = row.start_phase3; durationDays = row.duration_phase3; }
      else return;

      if (!start) return; // noch nicht initialisiert

      const end = addDays(new Date(start), Number(durationDays));

      // Noch nicht faellig
      if (now < end) return;

      // Faellig: advance oder reset
      if (row.phase < 3) {
        await advancePhaseAndEvaluate(daysForMinVotes);
      } else {
        await startPhases();
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
