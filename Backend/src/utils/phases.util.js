// src/utils/phases.util.js
import pool from "../services/mysql.service.js";

async function getActiveCycle() {
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
  return row || null;
}

async function getActiveCycleForUpdate(connection) {
  const [[row]] = await connection.query(`
    SELECT
      id, phase, aktiv,
      start_phase0, start_phase1, start_phase2, start_phase3,
      duration_phase0, duration_phase1, duration_phase2, duration_phase3
    FROM phases
    WHERE aktiv = 1
    ORDER BY id DESC
    LIMIT 1
    FOR UPDATE
  `);
  return row || null;
}

export async function getCurrentPhase() {
  const cycle = await getActiveCycle();
  if (!cycle) throw new Error("phases_row_missing");
  return cycle.phase;
}

export async function getActivePlayersCount(days = 10) {
  const seconds = days * 24 * 60 * 60;
  const [[{ activePlayers }]] = await pool.query(
    `
    SELECT COUNT(*) AS activePlayers
    FROM authme
    WHERE lastlogin >= (UNIX_TIMESTAMP() - ?) * 1000
    `,
    [seconds]
  );
  return activePlayers;
}

export async function getMinVotes(days = 10) {
  const activePlayers = await getActivePlayersCount(days);
  const minVotes = Math.ceil(activePlayers / 3);
  return { activePlayers, minVotes };
}

export async function startPhases() {
  const connection = await pool.getConnection();
  try {
    await connection.beginTransaction();

    const active = await getActiveCycleForUpdate(connection);

    const d0 = active ? Number(active.duration_phase0) : 4;
    const d1 = active ? Number(active.duration_phase1) : 2;
    const d2 = active ? Number(active.duration_phase2) : 4;
    const d3 = active ? Number(active.duration_phase3) : 2;

    if (![d0, d1, d2, d3].every((n) => Number.isInteger(n) && n >= 0)) {
      throw new Error("invalid_phase_durations");
    }

    if (active) {
      await connection.query("UPDATE phases SET aktiv = 0 WHERE id = ?", [
        active.id,
      ]);
    }

    const [ins] = await connection.query(
      `
      INSERT INTO phases (
        phase,
        start_phase0, start_phase1, start_phase2, start_phase3,
        duration_phase0, duration_phase1, duration_phase2, duration_phase3,
        aktiv
      )
      VALUES (
        0,
        NOW(),
        DATE_ADD(NOW(), INTERVAL ? DAY),
        DATE_ADD(NOW(), INTERVAL ? DAY),
        DATE_ADD(NOW(), INTERVAL ? DAY),
        ?, ?, ?, ?,
        1
      )
      `,
      [d0, d0 + d1, d0 + d1 + d2, d0, d1, d2, d3]
    );

    await connection.commit();
    return { ok: true, newCycleId: ins.insertId };
  } catch (err) {
    await connection.rollback();
    throw err;
  } finally {
    connection.release();
  }
}

export async function advancePhaseAndEvaluate(days = 10) {
  const connection = await pool.getConnection();
  try {
    await connection.beginTransaction();

    const active = await getActiveCycleForUpdate(connection);
    if (!active) {
      await connection.rollback();
      return { changed: false, error: "no_active_phase_cycle" };
    }

    const currentPhase = active.phase;

    if (currentPhase >= 3) {
      await connection.rollback();
      return { phase: currentPhase, changed: false, cycleId: active.id };
    }

    const { activePlayers, minVotes } = await getMinVotes(days);

    const [initiatives] = await connection.query(
      "SELECT id, status FROM initiatives WHERE aktiv = 1"
    );

    if (currentPhase === 0) {
      for (const ini of initiatives) {
        const [[{ cnt }]] = await connection.query(
          "SELECT COUNT(*) AS cnt FROM votes WHERE initiative_id = ?",
          [ini.id]
        );

        if (cnt < minVotes) {
          await connection.query(
            "UPDATE initiatives SET aktiv = 0, status = 0 WHERE id = ?",
            [ini.id]
          );
        } else {
          await connection.query(
            "UPDATE initiatives SET status = 0 WHERE id = ?",
            [ini.id]
          );
        }
      }
    } else if (currentPhase === 1) {
      // Admin-Votes sind in admin_votes
      for (const ini of initiatives) {
        const [[{ ja }]] = await connection.query(
          `
          SELECT COUNT(*) AS ja
          FROM admin_votes
          WHERE initiative_id = ?
            AND stimme = 1
          `,
          [ini.id]
        );

        if (ja < minVotes) {
          await connection.query(
            "UPDATE initiatives SET aktiv = 0, status = 1 WHERE id = ?",
            [ini.id]
          );
        } else {
          await connection.query(
            "UPDATE initiatives SET status = 1 WHERE id = ?",
            [ini.id]
          );
        }
      }
    } else if (currentPhase === 2) {
      // Spieler-Votes sind in player_votes
      for (const ini of initiatives) {
        const [[{ ja }]] = await connection.query(
          `
          SELECT COUNT(*) AS ja
          FROM player_votes
          WHERE initiative_id = ?
            AND stimme = 1
          `,
          [ini.id]
        );

        if (ja < minVotes) {
          await connection.query(
            "UPDATE initiatives SET aktiv = 0, status = 2 WHERE id = ?",
            [ini.id]
          );
        } else {
          await connection.query(
            "UPDATE initiatives SET status = 3 WHERE id = ?",
            [ini.id]
          );
        }
      }
    }

    const newPhase = currentPhase + 1;

    const fieldName =
      newPhase === 1
        ? "start_phase1"
        : newPhase === 2
        ? "start_phase2"
        : "start_phase3";

    await connection.query(
      `UPDATE phases SET phase = ?, ${fieldName} = NOW() WHERE id = ?`,
      [newPhase, active.id]
    );

    await connection.commit();
    return {
      phase: newPhase,
      changed: true,
      cycleId: active.id,
      activePlayers,
      minVotes,
    };
  } catch (err) {
    await connection.rollback();
    throw err;
  } finally {
    connection.release();
  }
}

export async function endCycleAndRestart() {
  return startPhases();
}
