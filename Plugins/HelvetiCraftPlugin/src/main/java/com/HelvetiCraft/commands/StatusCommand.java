package com.HelvetiCraft.commands;

import com.HelvetiCraft.Main;
import com.HelvetiCraft.initiatives.Initiative;
import com.HelvetiCraft.requests.InitiativeRequests;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class StatusCommand implements CommandExecutor {

    private final Main plugin;

    public StatusCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("helveticraft.status")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        sender.sendMessage("§6--- §bHelvetiCraft Projektstatus §6---");

        String stage = plugin.getConfig().getString("PROJECT_STAGE", "Unbekannt");
        String nextUpdate = plugin.getConfig().getString("NEXT_UPDATE", "Noch nicht festgelegt");

        sender.sendMessage("§aProjektphase: §e" + stage);
        sender.sendMessage("§aNächstes Update: §e" + nextUpdate);

        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        sender.sendMessage("§aServer Online: §e" + online + "/" + max);

        // Fetch from InitiativeRequests instead of InitiativeManager
        int totalInitiatives = InitiativeRequests.getAllInitiatives().size();
        int totalVotes = InitiativeRequests.getAllInitiatives().stream()
                .mapToInt(Initiative::getVotes)
                .sum();

        sender.sendMessage("§aGesamtanzahl Volksinitiativen: §e" + totalInitiatives);
        sender.sendMessage("§aGesamtanzahl Stimmen: §e" + totalVotes);

        sender.sendMessage("§7Diese Informationen sind relevant für die IDPA-Projektarbeit.");
        return true;
    }
}
