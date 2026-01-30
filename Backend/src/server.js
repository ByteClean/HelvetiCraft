// src/server.js
import dotenv from "dotenv";
dotenv.config();

import app from "./app.js";
import { startPhaseScheduler } from "./jobs/phases.scheduler.js";

const PORT = process.env.PORT || 3000;

app.listen(PORT, "0.0.0.0", () => {
  console.log(`API laeuft auf Port ${PORT}`);

  // Scheduler erst starten, wenn der Server wirklich hochgefahren ist
  startPhaseScheduler({
    daysForMinVotes: 10,
  });
});
