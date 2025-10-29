package com.HelvetiCraft.commands;

import com.HelvetiCraft.Claims.ClaimManager;
import com.HelvetiCraft.requests.ClaimRequests;
import com.HelvetiCraft.finance.FinanceManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SellClaimBlockCommand implements CommandExecutor {

    private final ClaimManager claimManager;

    public SellClaimBlockCommand(ClaimManager claimManager) {
        this.claimManager = claimManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cNur Spieler.");
            return true;
        }
        if (!sender.hasPermission("helveticraft.claims.sell")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("§eVerwendung: /sellclaimblock <Anzahl>");
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

        // Check remaining claims via PlaceholderAPI
        int remaining = claimManager.getRemainingClaims(p.getUniqueId());
        if (remaining < 0) {
            sender.sendMessage("§cFehler beim Prüfen deiner verfügbaren ClaimBlocks.");
            return true;
        }
        if (remaining < amount) {
            sender.sendMessage("§cDu hast nicht genügend unbenutzte ClaimBlocks (verfügbar: " + remaining + ").");
            return true;
        }

        boolean ok = claimManager.sellClaimBlocks(p.getUniqueId(), amount);
        if (!ok) {
            sender.sendMessage("§cVerkauf fehlgeschlagen: Interner Fehler oder Regierungskonto hat nicht genug Guthaben.");
            return true;
        }

        long payout = Math.multiplyExact(amount, ClaimRequests.getSellPriceCents());
        sender.sendMessage("§aErfolgreich verkauft: §f" + amount + " §aClaimBlocks für §f" + FinanceManager.formatCents(payout));
        return true;
    }
}