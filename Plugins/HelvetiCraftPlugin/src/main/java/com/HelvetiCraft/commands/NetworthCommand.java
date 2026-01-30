package com.HelvetiCraft.commands;

import com.HelvetiCraft.finance.FinanceManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class NetworthCommand implements CommandExecutor {

    private final FinanceManager finance;
    private static final UUID GOVERNMENT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final String GOVERNMENT_NAME = "§6§lStaatsfonds";

    public NetworthCommand(FinanceManager finance) {
        this.finance = finance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("helveticraft.networth")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }

        sender.sendMessage("§6§l═════ §b§lNet Worth §6§l═════");

        // 1. Gesamtkapital aller PRIVATEN Spieler (exkl. Staatsfonds)
        long governmentBalance = finance.getMain(GOVERNMENT_UUID);
        long totalPrivate = finance.getTotalNetWorthCents() - governmentBalance; // Annahme: diese Methode schließt Staatsfonds aus
        sender.sendMessage("§7Gesamtkapital (alle Bürger): §a" + FinanceManager.formatCents(totalPrivate));

        // 2. Staatsfonds separat anzeigen (auch wenn Konto nicht existiert → 0 anzeigen)

        if (governmentBalance == 0 && !finance.hasAccount(GOVERNMENT_UUID)) {
            finance.ensureAccount(GOVERNMENT_UUID); // Optional: erzwinge Konto
            governmentBalance = 0;
        }

        sender.sendMessage("§7Staatsfonds: " + GOVERNMENT_NAME + " §7- §a" + FinanceManager.formatCents(governmentBalance));

        // 3. Gesamtvermögen inkl. Staat
        long totalWithState = totalPrivate + governmentBalance;
        sender.sendMessage("§8Gesamtvermögen im Umlauf: §e" + FinanceManager.formatCents(totalWithState));

        sender.sendMessage(""); // Leerzeile
        sender.sendMessage("§6§l-- §e§lTop 8 Bürger §6§l--");

        // Alle Spieler außer Staatsfonds sammeln
        Map<UUID, Long> playerWorths = new HashMap<>();
        for (UUID id : finance.getKnownPlayers()) {
            if (id.equals(GOVERNMENT_UUID)) continue; // Staat ausschließen

            // Ensure player has a finance account (safety check)
            finance.ensureAccount(id);

            long worth = finance.getMain(id) + finance.getSavings(id);
            playerWorths.put(id, worth);
        }

        // Sortiert absteigend
        List<Map.Entry<UUID, Long>> sorted = playerWorths.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        // Top 8 anzeigen
        int rank = 1;
        for (Map.Entry<UUID, Long> entry : sorted.stream().limit(8).collect(Collectors.toList())) {
            String name = finance.getPlayerNameOrUuid(entry.getKey());
            sender.sendMessage("§e#" + rank + " §b" + name + " §7- §a" + FinanceManager.formatCents(entry.getValue()));
            rank++;
        }

        // Eigene Platzierung (nur wenn Spieler)
        if (sender instanceof Player player) {
            UUID playerId = player.getUniqueId();
            if (playerId.equals(GOVERNMENT_UUID)) return true; // Sollte nie passieren

            long playerWorth = finance.getMain(playerId) + finance.getSavings(playerId);
            int playerRank = -1;

            for (int i = 0; i < sorted.size(); i++) {
                if (sorted.get(i).getKey().equals(playerId)) {
                    playerRank = i + 1;
                    break;
                }
            }

            if (playerRank > 8 || playerRank == -1) {
                sender.sendMessage(""); // Leerzeile
                sender.sendMessage("§6§l-- §e§lDeine Platzierung §6§l--");
                sender.sendMessage("§e#" + (playerRank == -1 ? "?" : playerRank) + " §b" + player.getName() +
                        " §7- §a" + FinanceManager.formatCents(playerWorth));
            }
        }

        sender.sendMessage("§6§m                              ");
        return true;
    }
}