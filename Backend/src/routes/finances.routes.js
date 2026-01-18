import { Router } from "express";
import pool from "../services/mysql.service.js";
import * as finances from "../services/finances.service.js";

const r = Router();

// Set main_cents for a user
r.post("/:uuid/setMain", async (req, res) => {
  const { cents } = req.body;
  const { uuid } = req.params;
  try {
    await pool.query(
      `UPDATE finances SET main_cents = ? WHERE uuid = ?`,
      [cents, uuid]
    );
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ ok: false, error: err.message });
  }
});

// Set savings_cents for a user
r.post("/:uuid/setSavings", async (req, res) => {
  const { cents } = req.body;
  const { uuid } = req.params;
  try {
    await pool.query(
      `UPDATE finances SET savings_cents = ? WHERE uuid = ?`,
      [cents, uuid]
    );
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ ok: false, error: err.message });
  }
});

// Add to main_cents for a user
r.post("/:uuid/addToMain", async (req, res) => {
  const { cents } = req.body;
  const { uuid } = req.params;
  try {
    await pool.query(
      `UPDATE finances SET main_cents = COALESCE(main_cents,0) + ? WHERE uuid = ?`,
      [cents, uuid]
    );
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ ok: false, error: err.message });
  }
});

// Get total net worth (all users)
r.get("/totalNetWorth", async (req, res) => {
  try {
    const [[row]] = await pool.query(
      `SELECT SUM(COALESCE(main_cents,0) + COALESCE(savings_cents,0)) AS totalNetWorth FROM finances`
    );
    res.json({ totalNetWorth: Number(row.totalNetWorth) });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Get all known players (from authme table)
r.get("/knownPlayers", async (req, res) => {
  try {
    const uuids = await finances.getKnownPlayers();
    res.json({ players: uuids });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Transfer main -> main (muss vor /:uuid stehen!)
r.post("/transfer", async (req, res) => {
  const { from, to, cents } = req.body;
  const ok = await finances.transferMain(from, to, cents);
  res.json({ ok });
});

// Networth (fixer Prefix, auch vor /:uuid)
r.get("/networth/:uuid", async (req, res, next) => {
  const { uuid } = req.params;

  try {
    const [[row]] = await pool.query(
      `
      SELECT
        COALESCE(main_cents, 0) +
        COALESCE(savings_cents, 0) AS networth
      FROM finances
      WHERE uuid = ?
      `,
      [uuid]
    );

    if (!row) return res.json({ uuid, networth: 0 });

    return res.json({ uuid, networth: Number(row.networth) });
  } catch (err) {
    next(err);
  }
});

// Balance (hot path)
r.get("/:uuid/balance", async (req, res) => {
  const { uuid } = req.params;
  const balance = await finances.getBalance(uuid);
  res.json(balance);
});

// Account existiert?
r.get("/:uuid/exists", async (req, res) => {
  const exists = await finances.hasAccount(req.params.uuid);
  res.json({ exists });
});

// Get username by UUID (from authme table)
r.get("/:uuid/username", async (req, res) => {
  const { uuid } = req.params;
  try {
    const [[row]] = await pool.query(
      "SELECT username FROM authme WHERE uuid = ?",
      [uuid]
    );
    if (!row) return res.status(404).json({ error: "user_not_found" });
    res.json({ uuid, username: row.username });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Account erstellen (ganz am Schluss!)
r.post("/:uuid", async (req, res) => {
  const { starterCents } = req.body;
  await finances.createAccount(req.params.uuid, starterCents ?? 0);
  res.json({ ok: true });
});

export default r;
