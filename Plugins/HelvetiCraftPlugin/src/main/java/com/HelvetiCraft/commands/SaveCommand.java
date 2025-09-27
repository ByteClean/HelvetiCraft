package com.HelvetiCraft.commands;

import com.HelvetiCraft.finance.FinanceManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SaveCommand implements CommandExecutor {

    private final FinanceManager finance;

    public SaveCommand(FinanceManager finance) {
        this.finance = finance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl benutzen.");
            return true;
        }
        if (!sender.hasPermission("helveticraft.save")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("§eBenutzung: /save <Betrag>");
            return true;
        }

        long cents;
        try {
            cents = FinanceManager.parseAmountToCents(args[0]);
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cUngültiger Betrag. Beispiel: 100 oder 12.34");
            return true;
        }

        boolean success;
        if (cents > 0) {
            success = finance.moveMainToSavings(p.getUniqueId(), cents);
            if (success) {
                sender.sendMessage("§a" + FinanceManager.formatCents(cents) + " von deinem Hauptkonto ins Sparkonto verschoben.");
            } else {
                sender.sendMessage("§cNicht genügend Geld auf deinem Hauptkonto.");
            }
        } else if (cents < 0) {
            long abs = Math.abs(cents);
            success = finance.moveSavingsToMain(p.getUniqueId(), abs);
            if (success) {
                sender.sendMessage("§a" + FinanceManager.formatCents(abs) + " von deinem Sparkonto ins Hauptkonto verschoben.");
            } else {
                sender.sendMessage("§cNicht genügend Geld auf deinem Sparkonto.");
            }
        } else {
            sender.sendMessage("§cBetrag darf nicht 0 sein.");
            return true;
        }

        if (success) {
            finance.save(); // persist changes
        }

        return true;
    }
}
