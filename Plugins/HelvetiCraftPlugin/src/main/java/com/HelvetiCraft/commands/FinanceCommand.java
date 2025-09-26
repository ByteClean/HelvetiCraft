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
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        if (!sender.hasPermission("helveticraft.finance")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }
        var acc = finance.getAccount(p.getUniqueId());
        sender.sendMessage("§6====== §bFinanzen §6======");
        sender.sendMessage("§7Hauptkonto: §a" + FinanceManager.formatCents(acc.main));
        sender.sendMessage("§7Sparkonto:  §a" + FinanceManager.formatCents(acc.savings));
        sender.sendMessage("§7Gesamt:     §a" + FinanceManager.formatCents(acc.main + acc.savings));
        return true;
    }
}