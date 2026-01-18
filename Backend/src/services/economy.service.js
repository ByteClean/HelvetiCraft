import pool from "./mysql.service.js";

// -------- table creation --------

export async function createEconomyTables() {
  // Konjunktur cycles table
  await pool.query(`
    CREATE TABLE IF NOT EXISTS konjunktur_cycles (
      id INT AUTO_INCREMENT PRIMARY KEY,
      cycle_number INT NOT NULL,
      start_date TIMESTAMP NOT NULL,
      end_date TIMESTAMP NOT NULL,
      bip_total BIGINT DEFAULT 0,
      bip_c BIGINT DEFAULT 0 COMMENT 'Spieler Einkauf',
      bip_g BIGINT DEFAULT 0 COMMENT 'Staatliche Konsumausgaben',
      bip_i BIGINT DEFAULT 0 COMMENT 'Bruttoinvestitionen',
      bif_rate DECIMAL(10, 4) DEFAULT 1.0000,
      total_money_supply BIGINT DEFAULT 0,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      INDEX idx_cycle_number (cycle_number),
      INDEX idx_dates (start_date, end_date)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);

  // BIP history table
  await pool.query(`
    CREATE TABLE IF NOT EXISTS bip_history (
      id INT AUTO_INCREMENT PRIMARY KEY,
      cycle_id INT NOT NULL,
      calculation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      bip_total BIGINT NOT NULL,
      bip_c BIGINT NOT NULL,
      bip_g BIGINT NOT NULL,
      bip_i BIGINT NOT NULL,
      bif_rate DECIMAL(10, 4) NOT NULL,
      money_supply BIGINT NOT NULL,
      INDEX idx_cycle_id (cycle_id),
      INDEX idx_date (calculation_date),
      FOREIGN KEY (cycle_id) REFERENCES konjunktur_cycles(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);

  // Item supply/demand table
  await pool.query(`
    CREATE TABLE IF NOT EXISTS item_supply_demand (
      id INT AUTO_INCREMENT PRIMARY KEY,
      item_type VARCHAR(100) NOT NULL,
      calculation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      period_start TIMESTAMP NOT NULL,
      period_end TIMESTAMP NOT NULL,
      buy_count INT DEFAULT 0 COMMENT 'Nachfrage',
      sell_count INT DEFAULT 0 COMMENT 'Angebot',
      buy_quantity BIGINT DEFAULT 0,
      sell_quantity BIGINT DEFAULT 0,
      total_buy_value BIGINT DEFAULT 0,
      total_sell_value BIGINT DEFAULT 0,
      demand_ratio DECIMAL(10, 4) DEFAULT 1.0000,
      recommended_price BIGINT DEFAULT 0,
      INDEX idx_item_type (item_type),
      INDEX idx_date (calculation_date),
      INDEX idx_period (period_start, period_end)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
}

// -------- konjunktur management --------

export async function getCurrentKonjunktur() {
  const [[current]] = await pool.query(
    `SELECT * FROM konjunktur_cycles 
     WHERE start_date <= NOW() AND end_date >= NOW() 
     ORDER BY id DESC LIMIT 1`
  );
  return current || null;
}

export async function createNewKonjunktur() {
  // Get the last cycle number
  const [[lastCycle]] = await pool.query(
    `SELECT cycle_number FROM konjunktur_cycles ORDER BY id DESC LIMIT 1`
  );
  const newCycleNumber = (lastCycle?.cycle_number || 0) + 1;

  // Calculate dates (14 days cycle)
  const startDate = new Date();
  const endDate = new Date(startDate);
  endDate.setDate(endDate.getDate() + 14);

  const [result] = await pool.query(
    `INSERT INTO konjunktur_cycles (cycle_number, start_date, end_date) VALUES (?, ?, ?)`,
    [newCycleNumber, startDate, endDate]
  );

  return result.insertId;
}

export async function initializeKonjunkturIfNeeded() {
  const current = await getCurrentKonjunktur();
  if (!current) {
    const id = await createNewKonjunktur();
    console.log(`[Economy] Created initial Konjunktur cycle with ID ${id}`);
    return id;
  }
  return current.id;
}

// -------- BIP calculation --------

export async function calculateBIP(cycleId, startDate, endDate) {
  // C: Spieler Einkauf (all shop transactions)
  const [[shopData]] = await pool.query(
    `SELECT 
      COALESCE(SUM(price_cents), 0) as total_shop_value,
      COUNT(*) as transaction_count
     FROM shop_transactions 
     WHERE created_at BETWEEN ? AND ?`,
    [startDate, endDate]
  );

  // Also include regular transactions with type 'TRANSFER' or similar
  const [[transferData]] = await pool.query(
    `SELECT COALESCE(SUM(cents), 0) as total_transfer_value
     FROM transactions 
     WHERE created_at BETWEEN ? AND ? 
     AND transaction_type IN ('TRANSFER', 'Shop-Buy-Transaction', 'Shop-Sell-Transaction')`,
    [startDate, endDate]
  );

  const bip_c = Number(shopData.total_shop_value) + Number(transferData.total_transfer_value);

  // G: Staatliche Konsumausgaben (government spending)
  // Look for transactions FROM government (from_uuid = government UUID)
  const GOVERNMENT_UUID = '00000000-0000-0000-0000-000000000000';
  const [[govSpending]] = await pool.query(
    `SELECT COALESCE(SUM(cents), 0) as total_spending
     FROM transactions 
     WHERE created_at BETWEEN ? AND ? 
     AND from_uuid = ?
     AND transaction_type NOT IN ('Shop-Tax', 'Land-Tax', 'Wealth-Tax')`,
    [startDate, endDate, GOVERNMENT_UUID]
  );

  const bip_g = Number(govSpending.total_spending);

  // I: Bruttoinvestitionen (taxes as investment, land purchases, etc.)
  const [[taxData]] = await pool.query(
    `SELECT COALESCE(SUM(cents), 0) as total_taxes
     FROM transactions 
     WHERE created_at BETWEEN ? AND ? 
     AND to_uuid = ?`,
    [startDate, endDate, GOVERNMENT_UUID]
  );

  const bip_i = Number(taxData.total_taxes);

  // Total BIP
  const bip_total = bip_c + bip_g + bip_i;

  return {
    bip_total,
    bip_c,
    bip_g,
    bip_i
  };
}

// -------- BIF calculation --------

export async function calculateBIF(currentBIP, previousBIP) {
  // Get total money supply (sum of all player balances)
  const [[currentMoney]] = await pool.query(
    `SELECT COALESCE(SUM(main_cents + savings_cents), 0) as total_money
     FROM finances`
  );

  const currentMoneySupply = Number(currentMoney.total_money);

  // Get previous money supply from last cycle
  const [[previousData]] = await pool.query(
    `SELECT total_money_supply, bip_total 
     FROM konjunktur_cycles 
     WHERE id < (SELECT MAX(id) FROM konjunktur_cycles)
     ORDER BY id DESC LIMIT 1`
  );

  if (!previousData) {
    // First cycle, no comparison possible
    return { bif_rate: 1.0, money_supply: currentMoneySupply };
  }

  const previousMoneySupply = Number(previousData.total_money_supply);
  const prevBIP = Number(previousData.bip_total);

  // Calculate changes
  const deltaMoney = currentMoneySupply - previousMoneySupply;
  const deltaBIP = currentBIP - prevBIP;

  // BIF = ΔGeldmenge / ΔBIP
  let bif_rate = 1.0;
  if (deltaBIP !== 0) {
    bif_rate = deltaMoney / deltaBIP;
  }

  // Clamp to reasonable values (0.5 to 2.0)
  bif_rate = Math.max(0.5, Math.min(2.0, bif_rate));

  return {
    bif_rate,
    money_supply: currentMoneySupply
  };
}

// -------- complete BIP & BIF update for cycle --------

export async function updateKonjunkturEconomics(cycleId, startDate, endDate) {
  // Calculate BIP
  const { bip_total, bip_c, bip_g, bip_i } = await calculateBIP(cycleId, startDate, endDate);

  // Calculate BIF
  const { bif_rate, money_supply } = await calculateBIF(bip_total);

  // Update konjunktur cycle
  await pool.query(
    `UPDATE konjunktur_cycles 
     SET bip_total = ?, bip_c = ?, bip_g = ?, bip_i = ?, 
         bif_rate = ?, total_money_supply = ?
     WHERE id = ?`,
    [bip_total, bip_c, bip_g, bip_i, bif_rate, money_supply, cycleId]
  );

  // Save to history
  await pool.query(
    `INSERT INTO bip_history (cycle_id, bip_total, bip_c, bip_g, bip_i, bif_rate, money_supply)
     VALUES (?, ?, ?, ?, ?, ?, ?)`,
    [cycleId, bip_total, bip_c, bip_g, bip_i, bif_rate, money_supply]
  );

  console.log(`[Economy] Updated Konjunktur #${cycleId}: BIP=${bip_total}, BIF=${bif_rate.toFixed(4)}`);

  return { bip_total, bip_c, bip_g, bip_i, bif_rate, money_supply };
}

// -------- supply & demand calculation --------

export async function calculateSupplyDemand(periodStart, periodEnd) {
  // Get aggregated data per item
  const [items] = await pool.query(
    `SELECT 
      item_type,
      SUM(CASE WHEN transaction_type = 'BUY' THEN 1 ELSE 0 END) as buy_count,
      SUM(CASE WHEN transaction_type = 'SELL' THEN 1 ELSE 0 END) as sell_count,
      SUM(CASE WHEN transaction_type = 'BUY' THEN quantity ELSE 0 END) as buy_quantity,
      SUM(CASE WHEN transaction_type = 'SELL' THEN quantity ELSE 0 END) as sell_quantity,
      SUM(CASE WHEN transaction_type = 'BUY' THEN price_cents ELSE 0 END) as total_buy_value,
      SUM(CASE WHEN transaction_type = 'SELL' THEN price_cents ELSE 0 END) as total_sell_value
     FROM shop_transactions
     WHERE created_at BETWEEN ? AND ?
     GROUP BY item_type`,
    [periodStart, periodEnd]
  );

  const results = [];

  for (const item of items) {
    // DR_i = (Demand + 1) / (Supply + 1)
    const demand = Number(item.buy_count);
    const supply = Number(item.sell_count);
    const demand_ratio = (demand + 1) / (supply + 1);

    // Calculate recommended price based on average and demand ratio
    const avgBuyPrice = item.buy_quantity > 0 ? item.total_buy_value / item.buy_quantity : 0;
    const avgSellPrice = item.sell_quantity > 0 ? item.total_sell_value / item.sell_quantity : 0;
    const basePrice = Math.max(avgBuyPrice, avgSellPrice);

    // Apply demand factor (logarithmic adjustment from document)
    const k = 0.04; // adjustment coefficient
    let demandFactor = 1.0;
    if (demand_ratio > 1) {
      demandFactor = 1 + k * Math.log2(demand_ratio);
    } else if (demand_ratio < 1) {
      demandFactor = 1 - k * Math.log2(1 / demand_ratio);
    }

    const recommended_price = Math.round(basePrice * demandFactor);

    // Insert into database
    await pool.query(
      `INSERT INTO item_supply_demand 
       (item_type, period_start, period_end, buy_count, sell_count, 
        buy_quantity, sell_quantity, total_buy_value, total_sell_value, 
        demand_ratio, recommended_price)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        item.item_type,
        periodStart,
        periodEnd,
        demand,
        supply,
        item.buy_quantity,
        item.sell_quantity,
        item.total_buy_value,
        item.total_sell_value,
        demand_ratio,
        recommended_price
      ]
    );

    results.push({
      item_type: item.item_type,
      demand,
      supply,
      demand_ratio,
      recommended_price
    });
  }

  console.log(`[Economy] Calculated supply/demand for ${results.length} items`);

  return results;
}

// -------- getters for current economic data --------

export async function getCurrentBIPData() {
  const current = await getCurrentKonjunktur();
  if (!current) return null;

  return {
    cycle_number: current.cycle_number,
    start_date: current.start_date,
    end_date: current.end_date,
    bip_total: Number(current.bip_total),
    bip_c: Number(current.bip_c),
    bip_g: Number(current.bip_g),
    bip_i: Number(current.bip_i),
    bif_rate: Number(current.bif_rate),
    money_supply: Number(current.total_money_supply)
  };
}

export async function getItemSupplyDemand(itemType = null, limit = 1) {
  let query = `
    SELECT * FROM item_supply_demand 
  `;
  
  const params = [];
  
  if (itemType) {
    query += ` WHERE item_type = ?`;
    params.push(itemType);
  }
  
  query += ` ORDER BY calculation_date DESC`;
  
  if (limit) {
    query += ` LIMIT ?`;
    params.push(limit);
  }

  const [rows] = await pool.query(query, params);
  
  return rows.map(row => ({
    item_type: row.item_type,
    calculation_date: row.calculation_date,
    period_start: row.period_start,
    period_end: row.period_end,
    buy_count: Number(row.buy_count),
    sell_count: Number(row.sell_count),
    buy_quantity: Number(row.buy_quantity),
    sell_quantity: Number(row.sell_quantity),
    demand_ratio: Number(row.demand_ratio),
    recommended_price: Number(row.recommended_price)
  }));
}

export async function getAllItemsSupplyDemand() {
  const [rows] = await pool.query(`
    SELECT isd.* FROM item_supply_demand isd
    INNER JOIN (
      SELECT item_type, MAX(calculation_date) as latest_date
      FROM item_supply_demand
      GROUP BY item_type
    ) latest ON isd.item_type = latest.item_type AND isd.calculation_date = latest.latest_date
    ORDER BY isd.item_type
  `);

  return rows.map(row => ({
    item_type: row.item_type,
    calculation_date: row.calculation_date,
    buy_count: Number(row.buy_count),
    sell_count: Number(row.sell_count),
    demand_ratio: Number(row.demand_ratio),
    recommended_price: Number(row.recommended_price)
  }));
}
