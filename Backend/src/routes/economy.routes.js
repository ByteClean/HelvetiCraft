import { Router } from "express";
import * as economy from "../services/economy.service.js";
import pool from "../services/mysql.service.js";

const r = Router();

// Get current BIP data (current Konjunktur cycle)
r.get("/bip/current", async (req, res) => {
  try {
    const data = await economy.getCurrentBIPData();
    if (!data) {
      return res.status(404).json({ error: "No active Konjunktur cycle found" });
    }
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Get BIP history
r.get("/bip/history", async (req, res) => {
  try {
    const limit = parseInt(req.query.limit) || 10;
    const [rows] = await pool.query(
      `SELECT * FROM bip_history ORDER BY calculation_date DESC LIMIT ?`,
      [limit]
    );
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Get current Konjunktur cycle info
r.get("/konjunktur/current", async (req, res) => {
  try {
    const current = await economy.getCurrentKonjunktur();
    if (!current) {
      return res.status(404).json({ error: "No active Konjunktur cycle" });
    }
    res.json({
      cycle_number: current.cycle_number,
      start_date: current.start_date,
      end_date: current.end_date,
      days_remaining: Math.ceil((new Date(current.end_date) - new Date()) / (1000 * 60 * 60 * 24)),
      bip_total: Number(current.bip_total),
      bif_rate: Number(current.bif_rate)
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Get all Konjunktur cycles
r.get("/konjunktur/history", async (req, res) => {
  try {
    const limit = parseInt(req.query.limit) || 20;
    const [rows] = await pool.query(
      `SELECT * FROM konjunktur_cycles ORDER BY id DESC LIMIT ?`,
      [limit]
    );
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Get supply/demand for specific item
r.get("/supply-demand/:itemType", async (req, res) => {
  try {
    const { itemType } = req.params;
    const limit = parseInt(req.query.limit) || 1;
    const data = await economy.getItemSupplyDemand(itemType, limit);
    
    if (data.length === 0) {
      return res.status(404).json({ error: "No data found for this item" });
    }
    
    res.json(limit === 1 ? data[0] : data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Get supply/demand for all items (latest calculation)
r.get("/supply-demand/all", async (req, res) => {
  try {
    const data = await economy.getAllItemsSupplyDemand();
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Get recommended price for an item
r.get("/price-recommendation/:itemType", async (req, res) => {
  try {
    const { itemType } = req.params;
    const quantity = parseInt(req.query.quantity) || 1;
    
    const [data] = await economy.getItemSupplyDemand(itemType, 1);
    
    if (!data) {
      return res.status(404).json({ error: "No price data available for this item" });
    }

    // Get current BIF for adjustment
    const bipData = await economy.getCurrentBIPData();
    const bif = bipData?.bif_rate || 1.0;

    // Calculate price with BIF adjustment
    const basePrice = data.recommended_price;
    const adjustedPrice = Math.round(basePrice * bif * quantity);

    res.json({
      item_type: itemType,
      base_price_per_unit: basePrice,
      quantity: quantity,
      bif_adjusted_price: adjustedPrice,
      bif_rate: bif,
      demand_ratio: data.demand_ratio,
      calculation_date: data.calculation_date
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Force recalculate supply/demand (admin only)
r.post("/supply-demand/recalculate", async (req, res) => {
  try {
    const { days } = req.body;
    const daysBack = days || 4;
    
    const endDate = new Date();
    const startDate = new Date(endDate);
    startDate.setDate(startDate.getDate() - daysBack);
    
    const results = await economy.calculateSupplyDemand(startDate, endDate);
    
    res.json({
      ok: true,
      items_calculated: results.length,
      period: { start: startDate, end: endDate }
    });
  } catch (err) {
    res.status(500).json({ ok: false, error: err.message });
  }
});

// Force update BIP/BIF for current cycle (admin only)
r.post("/bip/recalculate", async (req, res) => {
  try {
    const current = await economy.getCurrentKonjunktur();
    if (!current) {
      return res.status(404).json({ error: "No active Konjunktur cycle" });
    }

    const result = await economy.updateKonjunkturEconomics(
      current.id,
      new Date(current.start_date),
      new Date()
    );

    res.json({ ok: true, ...result });
  } catch (err) {
    res.status(500).json({ ok: false, error: err.message });
  }
});

export default r;
