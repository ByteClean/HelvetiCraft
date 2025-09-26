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
        financeManager.ensureAccount(e.getPlayer().getUniqueId());
        // Optionally seed starting balance (example: 100.00)
        // financeManager.setMain(e.getPlayer().getUniqueId(), 10000L);
    }
}