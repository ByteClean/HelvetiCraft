// src/server.js
import dotenv from "dotenv";
dotenv.config();

import app from "./app.js";

import { startPhaseScheduler } from "./jobs/phases.scheduler.js";
import { startEconomyScheduler } from "./jobs/economy.scheduler.js";

import {
  createShopTransactionsTable,
  createTaxConfigTable,
  initializeTaxConfig,
} from "./services/finances.service.js";

import { createEconomyTables } from "./services/economy.service.js";

const PORT = process.env.PORT || 3000;

// Initialize database tables sequentially, then start server + schedulers
async function initializeDatabaseAndStart() {
  try {
    console.log("[Server] Initializing database tables...");

    await createShopTransactionsTable();
    console.log("[Server] shop_transactions table ready");

    await createTaxConfigTable();
    await initializeTaxConfig();
    console.log("[Server] tax_config table ready with default values");

    await createEconomyTables();
    console.log(
      "[Server] economy tables ready (konjunktur_cycles, bip_history, item_supply_demand)"
    );

    // Start app AFTER tables are ready
    app.listen(PORT, "0.0.0.0", () => {
      console.log(`[Server] API laeuft auf Port ${PORT}`);

      // Start schedulers AFTER server is listening (and DB is ready)
      startPhaseScheduler({ daysForMinVotes: 10 });
      startEconomyScheduler();
    });
  } catch (err) {
    console.error("[Server] Failed to initialize database:", err);
    process.exit(1);
  }
}

initializeDatabaseAndStart();
