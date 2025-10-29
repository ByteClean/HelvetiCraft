package com.HelvetiCraft.commands;

import com.HelvetiCraft.Claims.ClaimManager;
import com.HelvetiCraft.finance.FinanceManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuyClaimBlockCommand implements CommandExecutor {

    private final ClaimManager claimManager;

    public BuyClaimBlockCommand(ClaimManager claimManager) {
        this.claimManager = claimManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cNur Spieler.");
            return true;
        }
        if (!sender.hasPermission("helveticraft.claims.buy")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("§eVerwendung: /buyclaimblock <Anzahl>");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cUngültige Zahl.");
            return true;
        }
        if (amount <= 0) {
            sender.sendMessage("§cAnzahl muss größer als 0 sein.");
            return true;
        }

        boolean ok = claimManager.buyClaimBlocks(p.getUniqueId(), amount);
        if (!ok) {
            sender.sendMessage("§cKauf fehlgeschlagen: Ungenügend Guthaben oder interner Fehler.");
            return true;
        }

        long cost = amount * ClaimManager.BUY_PRICE_CENTS_PER_BLOCK;
        sender.sendMessage("§aErfolgreich gekauft: §f" + amount + " §aClaimBlocks für §f" + FinanceManager.formatCents(cost));
        return true;
    }
}
