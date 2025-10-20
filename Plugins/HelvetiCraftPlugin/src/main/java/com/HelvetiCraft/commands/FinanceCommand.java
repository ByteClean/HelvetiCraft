package com.HelvetiCraft.commands;

import com.HelvetiCraft.finance.FinanceManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FinanceCommand implements CommandExecutor {

    private final FinanceManager finance;

    public FinanceCommand(FinanceManager finance) {
        this.finance = finance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl verwenden.");
            return true;
        }
        if (!sender.hasPermission("helveticraft.finance")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }

        // Ensure account exists in backend (dummy for now)
        finance.ensureAccount(p.getUniqueId());

        long main = finance.getMain(p.getUniqueId());
        long savings = finance.getSavings(p.getUniqueId());
        long total = main + savings;

        sender.sendMessage("§6====== §bFinanzen §6======");
        sender.sendMessage("§7Hauptkonto: §a" + FinanceManager.formatCents(main));
        sender.sendMessage("§7Sparkonto:  §a" + FinanceManager.formatCents(savings));
        sender.sendMessage("§7Gesamt:     §a" + FinanceManager.formatCents(total));
        return true;
    }
}
