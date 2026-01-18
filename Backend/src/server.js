// src/server.js
import dotenv from "dotenv";
dotenv.config();

import app from "./app.js";
import { startPhaseScheduler } from "./jobs/phases.scheduler.js";
import { createShopTransactionsTable } from "./services/finances.service.js";

const PORT = process.env.PORT || 3000;

// Initialize database tables
createShopTransactionsTable()
  .then(() => console.log("shop_transactions table ready"))
  .catch(err => console.error("Failed to create shop_transactions table:", err));

app.listen(PORT, "0.0.0.0", () => {
  console.log(`API laeuft auf Port ${PORT}`);
});

// 12 Stunden = 12 * 60 * 60 * 1000
startPhaseScheduler({
  daysForMinVotes: 10,
});
