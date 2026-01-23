import { Router } from "express";
import pool from "../services/mysql.service.js";
import * as finances from "../services/finances.service.js";
import * as economy from "../services/economy.service.js";

const ORE_KEYS = [
  "COAL_ORE_CONVERSION",
  "IRON_ORE_CONVERSION",
  "COPPER_ORE_CONVERSION",
  "GOLD_ORE_CONVERSION",
  "REDSTONE_ORE_CONVERSION",
  "LAPIS_ORE_CONVERSION",
  "DIAMOND_ORE_CONVERSION",
  "EMERALD_ORE_CONVERSION",
  "QUARTZ_ORE_CONVERSION",
  "ANCIENT_DEBRIS_CONVERSION",
];

function applyBifToOreConfig(result, bifRate) {
  if (!bifRate || !Number.isFinite(bifRate)) return result;
  for (const key of ORE_KEYS) {
    if (result[key] !== undefined) {
      const base = Number(result[key]);
      if (Number.isFinite(base)) {
        result[key] = Math.round(base * bifRate);
      }
    }
  }
  return result;
}

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

// Log a transaction
r.post("/transactions/log", async (req, res) => {
  const { from, to, cents, transactionType } = req.body;
  try {
    await finances.logTransaction(
      from === "null" || !from ? null : from,
      to === "null" || !to ? null : to,
      cents,
      transactionType
    );
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ ok: false, error: err.message });
  }
});

// Log a shop transaction
r.post("/shop-transactions/log", async (req, res) => {
  const { itemType, quantity, transactionType, priceCents, buyerUuid, sellerUuid } = req.body;
  try {
    await finances.logShopTransaction(
      itemType,
      quantity,
      transactionType,
      priceCents,
      buyerUuid === "null" || !buyerUuid ? null : buyerUuid,
      sellerUuid === "null" || !sellerUuid ? null : sellerUuid
    );
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ ok: false, error: err.message });
  }
});

// ====== TAX CONFIG ENDPOINTS ======

// Get all tax config
r.get("/tax-config/all", async (req, res) => {
  try {
    const configs = await finances.getAllTaxConfig();
    const result = {};
    configs.forEach(cfg => {
      if (cfg.config_type === "json") {
        result[cfg.config_key] = JSON.parse(cfg.config_value);
      } else if (cfg.config_type === "number") {
        result[cfg.config_key] = parseFloat(cfg.config_value);
      } else {
        result[cfg.config_key] = cfg.config_value;
      }
    });

    // BIF-adjust ore conversions
    const bipData = await economy.getCurrentBIPData();
    const bif = bipData?.bif_rate || 1.0;
    applyBifToOreConfig(result, bif);

    res.json(result);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Get single tax config
r.get("/tax-config/:key", async (req, res) => {
  const { key } = req.params;
  try {
    const config = await finances.getTaxConfig(key);
    if (!config) return res.status(404).json({ error: "config_not_found" });

    let value = config.config_value;
    if (config.config_type === "json") {
      value = JSON.parse(value);
    } else if (config.config_type === "number") {
      value = parseFloat(value);
    }

    // BIF-adjust ore conversions
    if (ORE_KEYS.includes(key)) {
      const bipData = await economy.getCurrentBIPData();
      const bif = bipData?.bif_rate || 1.0;
      if (Number.isFinite(value) && Number.isFinite(bif)) {
        value = Math.round(Number(value) * bif);
      }
    }

    res.json({ key: config.config_key, value, type: config.config_type });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Update single tax config
r.post("/tax-config/:key", async (req, res) => {
  const { key } = req.params;
  const { value, type } = req.body;
  try {
    // Validate JSON type
    if (type === 'json' && typeof value === 'object') {
      await finances.updateTaxConfig(key, JSON.stringify(value), 'json');
    } else if (type === 'number') {
      await finances.updateTaxConfig(key, String(value), 'number');
    } else {
      await finances.updateTaxConfig(key, String(value), type || 'string');
    }
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ ok: false, error: err.message });
  }
});

// Account erstellen (ganz am Schluss!)
r.post("/:uuid", async (req, res) => {
  const { starterCents } = req.body;
  await finances.createAccount(req.params.uuid, starterCents ?? 0);
  res.json({ ok: true });
});

export default r;
