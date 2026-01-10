// src/utils/phases.util.js
import pool from "../services/mysql.service.js";

/**
 * Globale Phase holen (0–3).
 */
export async function getCurrentPhase() {
  const [[row]] = await pool.query(
    "SELECT phase, start_phase0, start_phase1, start_phase2, start_phase3 FROM phases WHERE id = 1"
  );
  if (!row) {
    throw new Error("phases_row_missing");
  }
  return row.phase; // 0,1,2,3
}

/**
 * Aktive Spieler in den letzten `days` Tagen.
 * lastlogin ist Unix-Timestamp in Millisekunden.
 */
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

/**
 * Mindeststimmen: ceil(activePlayers / 3)
 */
export async function getMinVotes(days = 10) {
  const activePlayers = await getActivePlayersCount(days);
  const minVotes = Math.ceil(activePlayers / 3);
  return { activePlayers, minVotes };
}

/**
 * Phase wechseln + Initiativen anhand der Phase-Regeln bewerten.
 *
 * Phasen:
 * 0 → Normales Voting (alle)
 * 1 → Admin JA/NEIN
 * 2 → Spieler JA/NEIN
 * 3 → akzeptiert (Endzustand, keine Bewertung mehr)
 *
 * Logik:
 * - Beim Wechsel 0→1: normale Votes prüfen
 * - Beim Wechsel 1→2: Admin-JA-Votes prüfen
 * - Beim Wechsel 2→3: Spieler-JA-Votes prüfen
 *
 * Initiativen, die minVotes NICHT erreichen:
 *   aktiv = 0
 *   status = aktuelle Phase
 *
 * Initiativen, die bestehen:
 *   status = nächste/letzte Phase (bei 2→3: status=3)
 */
export async function advancePhaseAndEvaluate(days = 10) {
  const connection = await pool.getConnection();
  try {
    await connection.beginTransaction();

    const [[ph]] = await connection.query(
      "SELECT phase FROM phases WHERE id = 1 FOR UPDATE"
    );
    if (!ph) throw new Error("phases_row_missing");

    const currentPhase = ph.phase;
    if (currentPhase >= 3) {
      // schon in akzeptiert
      await connection.rollback();
      return { phase: currentPhase, changed: false };
    }

    const { activePlayers, minVotes } = await getMinVotes(days);

    // alle aktuell aktiven Initiativen holen
    const [initiatives] = await connection.query(
      "SELECT id, status FROM initiatives WHERE aktiv = 1"
    );

    if (currentPhase === 0) {
      // Ende normales Voting – normale Votes prüfen
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
      // Ende Admin-Voting – nur Admin-JA-Stimmen zählen
      for (const ini of initiatives) {
        const [[{ ja }]] = await connection.query(
          `
          SELECT COUNT(*) AS ja
          FROM final_votes fv
          JOIN authme a ON a.id = fv.user_id
          WHERE fv.initiative_id = ?
            AND fv.stimme = 1
            AND a.isAdmin = 1
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
      // Ende Spieler-Finalvoting – alle JA-Stimmen zählen
      for (const ini of initiatives) {
        const [[{ ja }]] = await connection.query(
          `
          SELECT COUNT(*) AS ja
          FROM final_votes
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

    // Phasen-Startzeit setzen
    const fieldName =
      newPhase === 1
        ? "start_phase1"
        : newPhase === 2
        ? "start_phase2"
        : "start_phase3";

    await connection.query(
      `UPDATE phases SET phase = ?, ${fieldName} = NOW() WHERE id = 1`,
      [newPhase]
    );

    await connection.commit();
    return {
      phase: newPhase,
      changed: true,
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

import pool from "../services/mysql.service.js";

export async function startPhases() {
  const connection = await pool.getConnection();
  try {
    await connection.beginTransaction();

    // Row locken / sicherstellen, dass id=1 existiert
    await connection.query(
      `
      INSERT INTO phases (id, phase, start_phase0, duration_phase0, duration_phase1, duration_phase2, duration_phase3, aktiv)
      VALUES (1, 0, NOW(), 4, 4, 4, 4, 1)
      ON DUPLICATE KEY UPDATE aktiv = 1
      `
    );

    // Durations lesen (FOR UPDATE)
    const [[row]] = await connection.query(
      `
      SELECT duration_phase0, duration_phase1, duration_phase2, duration_phase3
      FROM phases
      WHERE id = 1
      FOR UPDATE
      `
    );

    if (!row) throw new Error("phases_row_missing");

    const d0 = Number(row.duration_phase0);
    const d1 = Number(row.duration_phase1);
    const d2 = Number(row.duration_phase2);
    const d3 = Number(row.duration_phase3);

    if (![d0, d1, d2, d3].every((n) => Number.isInteger(n) && n >= 0)) {
      throw new Error("invalid_phase_durations");
    }

    // Startzeiten setzen: Annahme = Durations sind TAGE
    await connection.query(
      `
      UPDATE phases
      SET
        phase = 0,
        aktiv = 1,
        start_phase0 = NOW(),
        start_phase1 = DATE_ADD(NOW(), INTERVAL ? DAY),
        start_phase2 = DATE_ADD(NOW(), INTERVAL ? DAY),
        start_phase3 = DATE_ADD(NOW(), INTERVAL ? DAY)
      WHERE id = 1
      `,
      [
        d0,
        d0 + d1,
        d0 + d1 + d2,
      ]
    );

    await connection.commit();
    return { ok: true };
  } catch (err) {
    await connection.rollback();
    throw err;
  } finally {
    connection.release();
  }
}
