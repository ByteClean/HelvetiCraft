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

        // Create account in dummy backend if missing
        if (!financeManager.hasAccount(id)) {
            financeManager.ensureAccount(id);
        }
    }
}
