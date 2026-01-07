import { Router } from "express";
import pool from "../services/mysql.service.js";
import * as finances from "../services/finances.service.js";
const r = Router();


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

// Account erstellen
r.post("/:uuid", async (req, res) => {
  const { starterCents } = req.body;
  await finances.createAccount(req.params.uuid, starterCents ?? 0);
  res.json({ ok: true });
});

// Transfer main -> main
r.post("/transfer", async (req, res) => {
  const { from, to, cents } = req.body;
  const ok = await finances.transferMain(from, to, cents);
  res.json({ ok });
});
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

    // Spieler hat noch keinen Account → 0
    if (!row) {
      return res.json({ uuid, networth: 0 });
    }

    return res.json({
      uuid,
      networth: Number(row.networth),
    });
  } catch (err) {
    next(err);
  }
});

export default r;
