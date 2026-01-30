// src/jobs/phases.scheduler.js
import pool from "../services/mysql.service.js";
import { advancePhaseAndEvaluate, endCycleAndRestart } from "../utils/phases.util.js";

function addDays(date, days) {
  return new Date(date.getTime() + days * 24 * 60 * 60 * 1000);
}

function msUntilNextFiveAM() {
  const now = new Date();
  const next = new Date(now);
  next.setHours(5, 0, 0, 0); // 05:00:00

  if (now >= next) next.setDate(next.getDate() + 1);

  return next.getTime() - now.getTime();
}

export function startPhaseScheduler({ daysForMinVotes = 10 } = {}) {
  const tick = async (reason = "scheduled") => {
    let conn;
    try {
      conn = await pool.getConnection();

      // nur ein Scheduler-Prozess darf laufen (wichtig bei mehreren Instanzen)
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

      console.log("[PHASE_SCHEDULER] due -> running", {
        reason,
        cycleId: row.id,
        phase: row.phase,
        now: now.toISOString(),
        end: end.toISOString(),
      });

      if (row.phase < 3) {
        await advancePhaseAndEvaluate(daysForMinVotes);
      } else {
        await endCycleAndRestart();
      }
    } catch (e) {
      console.error("[PHASE_SCHEDULER] error", e?.message || e);
    } finally {
      if (conn) {
        try { await conn.query("SELECT RELEASE_LOCK('phases_scheduler')"); } catch {}
        try { conn.release(); } catch {}
      }
    }
  };

  // 1) Sofortiger Check nach Neustart (direkt wirksam)
  tick("startup");

  // 2) Danach fixe Uhrzeit 05:00 jeden Tag
  const delayMs = msUntilNextFiveAM();
  console.log(`[PHASE_SCHEDULER] next scheduled run in ${Math.round(delayMs / 1000)}s (05:00)`);

  setTimeout(() => {
    tick("daily_05_00");

    setInterval(() => {
      tick("daily_05_00");
    }, 24 * 60 * 60 * 1000);
  }, delayMs);
}
