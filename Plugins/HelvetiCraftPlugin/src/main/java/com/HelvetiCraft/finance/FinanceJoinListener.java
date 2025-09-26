package com.HelvetiCraft.finance;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class FinanceJoinListener implements Listener {

    private final FinanceManager financeManager;

    public FinanceJoinListener(FinanceManager financeManager) {
        this.financeManager = financeManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        java.util.UUID id = e.getPlayer().getUniqueId();
        // If no account exists yet (first join), seed starter money of 250.00 (25000 cents)
        if (!financeManager.hasAccount(id)) {
            financeManager.ensureAccount(id);
            financeManager.setMain(id, 25000L);
            financeManager.save();
            return;
        }
        // Ensure account exists in cache for returning players as well
        financeManager.ensureAccount(id);
    }
}