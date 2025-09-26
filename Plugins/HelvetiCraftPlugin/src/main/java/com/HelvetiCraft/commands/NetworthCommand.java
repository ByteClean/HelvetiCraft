package com.HelvetiCraft.commands;

import com.HelvetiCraft.finance.FinanceManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class NetworthCommand implements CommandExecutor {

    private final FinanceManager finance;

    public NetworthCommand(FinanceManager finance) {
        this.finance = finance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("helveticraft.networth")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }
        long total = finance.getTotalNetWorthCents();
        sender.sendMessage("§6====== §bNet Worth §6======");
        sender.sendMessage("§7Gesamtkapital (alle Spieler, Konten): §a" + FinanceManager.formatCents(total));
        sender.sendMessage("§8(Hinweis: Item-Verkauf fließt später ein)");
        return true;
    }
}