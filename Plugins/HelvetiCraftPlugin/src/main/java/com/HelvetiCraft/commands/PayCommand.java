package com.HelvetiCraft.commands;

import com.HelvetiCraft.finance.FinanceManager;
import com.HelvetiCraft.util.FinanceTransactionLogger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PayCommand implements CommandExecutor, TabCompleter {

    private final FinanceManager finance;

    public PayCommand(FinanceManager finance) {
        this.finance = finance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player fromPlayer)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        if (!sender.hasPermission("helveticraft.pay")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§eVerwendung: /pay <Spieler> <Betrag>");
            return true;
        }

        Player toPlayer = Bukkit.getPlayerExact(args[0]);
        if (toPlayer == null) {
            sender.sendMessage("§cSpieler nicht gefunden oder offline.");
            return true;
        }
        if (toPlayer.getUniqueId().equals(fromPlayer.getUniqueId())) {
            sender.sendMessage("§cDu kannst dir nicht selbst Geld senden.");
            return true;
        }

        long cents;
        try {
            cents = FinanceManager.parseAmountToCents(args[1]);
        } catch (IllegalArgumentException ex) {
            sender.sendMessage("§cUngültiger Betrag. Beispiel: 12.34");
            return true;
        }
        if (cents <= 0) {
            sender.sendMessage("§cBetrag muss größer als 0 sein.");
            return true;
        }

        UUID from = fromPlayer.getUniqueId();
        UUID to = toPlayer.getUniqueId();

        FinanceTransactionLogger logger = new FinanceTransactionLogger(finance);
        logger.logTransaction("Transfer", from, to, cents);
        //boolean ok = finance.transferMain(from, to, cents);
        boolean ok = true;
        if (!ok) {
            sender.sendMessage("§cUnzureichender Kontostand auf deinem Hauptkonto.");
            return true;
        }

        String formatted = FinanceManager.formatCents(cents);
        sender.sendMessage("§aÜberwiesen: §f" + formatted + " §aan §f" + toPlayer.getName());
        toPlayer.sendMessage("§aErhalten: §f" + formatted + " §avon §f" + fromPlayer.getName());

        finance.save(); // persist after transaction
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> res = new ArrayList<>();
        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (sender.getName().equalsIgnoreCase(p.getName())) continue;
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    res.add(p.getName());
                }
            }
        }
        return res;
    }
}