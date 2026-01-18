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

// -------- transaction logging --------

export async function logTransaction(fromUuid, toUuid, cents, transactionType) {
  await pool.query(
    "INSERT INTO transactions (from_uuid, to_uuid, cents, transaction_type, created_at) VALUES (?, ?, ?, ?, NOW())",
    [fromUuid || null, toUuid || null, cents, transactionType]
  );
}

// -------- shop transaction logging --------

export async function createShopTransactionsTable() {
  await pool.query(`
    CREATE TABLE IF NOT EXISTS shop_transactions (
      id INT AUTO_INCREMENT PRIMARY KEY,
      item_type VARCHAR(100) NOT NULL,
      quantity INT NOT NULL,
      transaction_type ENUM('BUY', 'SELL') NOT NULL,
      price_cents BIGINT NOT NULL,
      buyer_uuid VARCHAR(36),
      seller_uuid VARCHAR(36),
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      INDEX idx_item_type (item_type),
      INDEX idx_created_at (created_at),
      INDEX idx_buyer (buyer_uuid),
      INDEX idx_seller (seller_uuid)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
}

export async function logShopTransaction(itemType, quantity, transactionType, priceCents, buyerUuid, sellerUuid) {
  await pool.query(
    "INSERT INTO shop_transactions (item_type, quantity, transaction_type, price_cents, buyer_uuid, seller_uuid, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())",
    [itemType, quantity, transactionType, priceCents, buyerUuid || null, sellerUuid || null]
  );
}

// -------- tax configuration --------

export async function createTaxConfigTable() {
  await pool.query(`
    CREATE TABLE IF NOT EXISTS tax_config (
      config_key VARCHAR(100) PRIMARY KEY,
      config_value VARCHAR(1000) NOT NULL,
      config_type ENUM('number', 'string', 'json') DEFAULT 'number',
      description VARCHAR(500),
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      INDEX idx_updated_at (updated_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
}

export async function initializeTaxConfig() {
  // Insert default tax values if they don't exist
  const defaults = [
    { key: 'MWST', value: '7.7', type: 'number', desc: 'Mehrwertsteuer (%)' },
    { key: 'VERKAUFS_STEUER_1ZU1', value: '26.0', type: 'number', desc: '1-zu-1-Verkaufssteuer (%)' },
    { key: 'SHOP_STEUER', value: '2.0', type: 'number', desc: 'Shopsteuer (%)' },
    { key: 'LAND_STEUER_BASIS_PER_BLOCK', value: '1.0', type: 'number', desc: 'Landsteuer Basis pro Block (cents)' },
    { key: 'LAND_STEUER_INTERVAL_DAYS', value: '3', type: 'number', desc: 'Landsteuer Intervall (Tage)' },
    { key: 'ORE_CONVERT_TAX', value: '500', type: 'number', desc: 'Ore Conversion Tax (cents)' },
    { key: 'EINKOMMEN_STEUER_BRACKETS', value: '[{"threshold":0,"rate":5},{"threshold":1000000,"rate":12},{"threshold":5000000,"rate":20},{"threshold":10000000,"rate":30}]', type: 'json', desc: 'Progressive Einkommen Steuer Brackets' },
    { key: 'VERMOEGENS_STEUER_BRACKETS', value: '[{"threshold":0,"rate":0.2},{"threshold":5000000,"rate":0.5},{"threshold":25000000,"rate":1.0}]', type: 'json', desc: 'Progressive Vermögens Steuer Brackets' },
    { key: 'COAL_ORE_CONVERSION', value: '50', type: 'number', desc: 'Coal Ore Conversion (cents)' },
    { key: 'IRON_ORE_CONVERSION', value: '100', type: 'number', desc: 'Iron Ore Conversion (cents)' },
    { key: 'COPPER_ORE_CONVERSION', value: '80', type: 'number', desc: 'Copper Ore Conversion (cents)' },
    { key: 'GOLD_ORE_CONVERSION', value: '300', type: 'number', desc: 'Gold Ore Conversion (cents)' },
    { key: 'REDSTONE_ORE_CONVERSION', value: '150', type: 'number', desc: 'Redstone Ore Conversion (cents)' },
    { key: 'LAPIS_ORE_CONVERSION', value: '200', type: 'number', desc: 'Lapis Ore Conversion (cents)' },
    { key: 'DIAMOND_ORE_CONVERSION', value: '1000', type: 'number', desc: 'Diamond Ore Conversion (cents)' },
    { key: 'EMERALD_ORE_CONVERSION', value: '1500', type: 'number', desc: 'Emerald Ore Conversion (cents)' },
    { key: 'QUARTZ_ORE_CONVERSION', value: '120', type: 'number', desc: 'Quartz Ore Conversion (cents)' },
    { key: 'ANCIENT_DEBRIS_CONVERSION', value: '2000', type: 'number', desc: 'Ancient Debris Conversion (cents)' }
  ];

  for (const def of defaults) {
    await pool.query(
      `INSERT IGNORE INTO tax_config (config_key, config_value, config_type, description) VALUES (?, ?, ?, ?)`,
      [def.key, def.value, def.type, def.desc]
    );
  }
}

export async function getTaxConfig(configKey) {
  const [[row]] = await pool.query(
    "SELECT config_key, config_value, config_type FROM tax_config WHERE config_key = ?",
    [configKey]
  );
  return row || null;
}

export async function getAllTaxConfig() {
  const [rows] = await pool.query(
    "SELECT config_key, config_value, config_type FROM tax_config ORDER BY config_key"
  );
  return rows;
}

export async function updateTaxConfig(configKey, configValue, configType = 'number') {
  await pool.query(
    "UPDATE tax_config SET config_value = ?, config_type = ? WHERE config_key = ?",
    [configValue, configType, configKey]
  );
}

export async function setTaxConfig(configKey, configValue, configType = 'number', description = null) {
  await pool.query(
    `INSERT INTO tax_config (config_key, config_value, config_type, description) 
     VALUES (?, ?, ?, ?) 
     ON DUPLICATE KEY UPDATE config_value = ?, config_type = ?, description = COALESCE(?, description)`,
    [configKey, configValue, configType, description, configValue, configType, description]
  );
}
