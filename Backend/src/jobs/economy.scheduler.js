import * as economy from "../services/economy.service.js";

let supplyDemandInterval = null;
let konjunkturCheckInterval = null;

/**
 * Starts the economic calculation schedulers:
 * - Supply/Demand: Every 4 days
 * - BIP/BIF: At end of 14-day Konjunktur cycle
 */
export function startEconomyScheduler() {
  console.log("[Economy Scheduler] Starting economy calculation schedulers...");

  // Initialize Konjunktur if needed
  economy.initializeKonjunkturIfNeeded()
    .then(() => console.log("[Economy Scheduler] Konjunktur cycle initialized"))
    .catch(err => console.error("[Economy Scheduler] Failed to initialize Konjunktur:", err));

  // Supply & Demand calculation: Every 4 days
  const FOUR_DAYS_MS = 4 * 24 * 60 * 60 * 1000;
  
  supplyDemandInterval = setInterval(async () => {
    try {
      console.log("[Economy Scheduler] Running supply/demand calculation...");
      
      const endDate = new Date();
      const startDate = new Date(endDate);
      startDate.setDate(startDate.getDate() - 4); // Last 4 days
      
      const results = await economy.calculateSupplyDemand(startDate, endDate);
      console.log(`[Economy Scheduler] Supply/demand calculated for ${results.length} items`);
    } catch (err) {
      console.error("[Economy Scheduler] Error in supply/demand calculation:", err);
    }
  }, FOUR_DAYS_MS);

  // Run once immediately
  (async () => {
    try {
      const endDate = new Date();
      const startDate = new Date(endDate);
      startDate.setDate(startDate.getDate() - 4);
      await economy.calculateSupplyDemand(startDate, endDate);
      console.log("[Economy Scheduler] Initial supply/demand calculation completed");
    } catch (err) {
      console.error("[Economy Scheduler] Initial supply/demand calculation failed:", err);
    }
  })();

  // Konjunktur cycle check: Every hour
  const ONE_HOUR_MS = 60 * 60 * 1000;
  
  konjunkturCheckInterval = setInterval(async () => {
    try {
      const current = await economy.getCurrentKonjunktur();
      
      if (!current) {
        console.log("[Economy Scheduler] No active Konjunktur, creating new one");
        await economy.createNewKonjunktur();
        return;
      }

      const now = new Date();
      const endDate = new Date(current.end_date);

      // Check if cycle has ended
      if (now >= endDate) {
        console.log(`[Economy Scheduler] Konjunktur cycle #${current.cycle_number} has ended, calculating final BIP & BIF...`);
        
        // Calculate final economics for completed cycle
        await economy.updateKonjunkturEconomics(
          current.id,
          new Date(current.start_date),
          endDate
        );

        // Create new cycle
        const newCycleId = await economy.createNewKonjunktur();
        console.log(`[Economy Scheduler] Started new Konjunktur cycle #${newCycleId}`);
      }
    } catch (err) {
      console.error("[Economy Scheduler] Error in Konjunktur check:", err);
    }
  }, ONE_HOUR_MS);

  console.log("[Economy Scheduler] Economy schedulers started successfully");
  console.log("  - Supply/Demand: Every 4 days");
  console.log("  - Konjunktur check: Every hour");
}

export function stopEconomyScheduler() {
  if (supplyDemandInterval) {
    clearInterval(supplyDemandInterval);
    supplyDemandInterval = null;
  }
  if (konjunkturCheckInterval) {
    clearInterval(konjunkturCheckInterval);
    konjunkturCheckInterval = null;
  }
  console.log("[Economy Scheduler] Economy schedulers stopped");
}
