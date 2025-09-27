package com.HelvetiCraft.commands;

import com.HelvetiCraft.finance.FinanceManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

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

        // Gesamtkapital aller Spieler
        long total = finance.getTotalNetWorthCents();
        sender.sendMessage("§6====== §bNet Worth §6======");
        sender.sendMessage("§7Gesamtkapital (alle Spieler, Konten): §a" + FinanceManager.formatCents(total));
        sender.sendMessage("§8(Hinweis: Item-Verkauf fließt später ein)");

        // Alle Spieler + Networth einsammeln
        Map<UUID, Long> worths = new HashMap<>();
        for (UUID id : finance.getKnownPlayers()) {
            FinanceManager.Account acc = finance.getAccount(id);
            long worth = acc.main + acc.savings;
            worths.put(id, worth);
        }

        // Sortieren nach Networth absteigend
        List<Map.Entry<UUID, Long>> sorted = worths.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        // Top 8 anzeigen
        sender.sendMessage("§6-- §eTop 8 Spieler §6--");
        int rank = 1;
        for (Map.Entry<UUID, Long> entry : sorted.stream().limit(8).collect(Collectors.toList())) {
            String name = finance.getPlayerNameOrUuid(entry.getKey());
            sender.sendMessage("§e#" + rank + " §b" + name + " §7- §a" + FinanceManager.formatCents(entry.getValue()));
            rank++;
        }

        // Eigene Position falls nicht in Top 8
        if (sender instanceof Player) {
            Player p = (Player) sender;
            UUID id = p.getUniqueId();
            int playerRank = -1;
            long playerWorth = 0L;

            for (int i = 0; i < sorted.size(); i++) {
                if (sorted.get(i).getKey().equals(id)) {
                    playerRank = i + 1;
                    playerWorth = sorted.get(i).getValue();
                    break;
                }
            }

            if (playerRank > 8) {
                sender.sendMessage("§6-- §eDeine Platzierung §6--");
                sender.sendMessage("§e#" + playerRank + " §b" + p.getName() + " §7- §a" + FinanceManager.formatCents(playerWorth));
            }
        }

        return true;
    }
}
