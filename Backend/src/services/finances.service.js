import pool from "./mysql.service.js";

// -------- account --------

export async function hasAccount(uuid) {
  const [[row]] = await pool.query(
    "SELECT 1 FROM finances WHERE uuid = ?",
    [uuid]
  );
  return !!row;
}

export async function createAccount(uuid, starterCents) {
  await pool.query(
    "INSERT IGNORE INTO finances (uuid, main_cents, savings_cents) VALUES (?, ?, 0)",
    [uuid, starterCents]
  );
}

// -------- balance (sehr haeufig) --------

export async function getBalance(uuid) {
  const [[row]] = await pool.query(
    "SELECT main_cents, savings_cents FROM finances WHERE uuid = ?",
    [uuid]
  );

  if (!row) {
    return { uuid, main: 0, savings: 0 };
  }

  return {
    uuid,
    main: Number(row.main_cents),
    savings: Number(row.savings_cents),
  };
}

// -------- transfer (atomar) --------

export async function transferMain(fromUuid, toUuid, cents) {
  if (cents <= 0) return false;

  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();

    const [[from]] = await conn.query(
      "SELECT main_cents FROM finances WHERE uuid = ? FOR UPDATE",
      [fromUuid]
    );

    if (!from || from.main_cents < cents) {
      await conn.rollback();
      return false;
    }

    await conn.query(
      "UPDATE finances SET main_cents = main_cents - ? WHERE uuid = ?",
      [cents, fromUuid]
    );

    await conn.query(
      "INSERT IGNORE INTO finances (uuid, main_cents, savings_cents) VALUES (?, 0, 0)",
      [toUuid]
    );

    await conn.query(
      "UPDATE finances SET main_cents = main_cents + ? WHERE uuid = ?",
      [cents, toUuid]
    );

    //await conn.query(
    //  "INSERT INTO transactions (from_uuid, to_uuid, cents, transaction_type) VALUES (?, ?, ?, 'TRANSFER')",
    //  [fromUuid, toUuid, cents]
    //);

    await conn.commit();
    return true;
  } catch (err) {
    await conn.rollback();
    throw err;
  } finally {
    conn.release();
  }
}

// -------- get all known players --------

export async function getKnownPlayers() {
  const [rows] = await pool.query(
    "SELECT uuid FROM authme WHERE uuid IS NOT NULL AND uuid != '00000000-0000-0000-0000-000000000000'"
  );
  return rows.map(row => row.uuid);
}
