// src/server.js
import dotenv from "dotenv";
dotenv.config();

import app from "./app.js";
import { startPhaseScheduler } from "./jobs/phases.scheduler.js";
import { startEconomyScheduler } from "./jobs/economy.scheduler.js";
import { createShopTransactionsTable, createTaxConfigTable, initializeTaxConfig } from "./services/finances.service.js";
import { createEconomyTables } from "./services/economy.service.js";

const PORT = process.env.PORT || 3000;

// Initialize database tables
createShopTransactionsTable()
  .then(() => console.log("shop_transactions table ready"))
  .catch(err => console.error("Failed to create shop_transactions table:", err));

createTaxConfigTable()
  .then(() => initializeTaxConfig())
  .then(() => console.log("tax_config table ready with default values"))
  .catch(err => console.error("Failed to create/initialize tax_config table:", err));

createEconomyTables()
  .then(() => console.log("economy tables ready (konjunktur_cycles, bip_history, item_supply_demand)"))
  .catch(err => console.error("Failed to create economy tables:", err));

app.listen(PORT, "0.0.0.0", () => {
  console.log(`API laeuft auf Port ${PORT}`);
});

// Start schedulers
startPhaseScheduler({
  daysForMinVotes: 10,
});

startEconomyScheduler();
