import dotenv from "dotenv";
dotenv.config();
import app from "./app.js";
import { startPhaseScheduler } from "./jobs/phases.scheduler.js";


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`API laeuft auf http://localhost:${PORT}`);
});
startPhaseScheduler({
  intervalMs:  1000, // 12 Stunden
  daysForMinVotes: 10,
});
