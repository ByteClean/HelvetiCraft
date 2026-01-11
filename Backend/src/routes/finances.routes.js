import { Router } from "express";
import pool from "../services/mysql.service.js";
import * as finances from "../services/finances.service.js";

const r = Router();

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

// Account erstellen (ganz am Schluss!)
r.post("/:uuid", async (req, res) => {
  const { starterCents } = req.body;
  await finances.createAccount(req.params.uuid, starterCents ?? 0);
  res.json({ ok: true });
});

export default r;
