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
