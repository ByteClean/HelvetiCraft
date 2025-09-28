package com.HelvetiCraft.commands;

import com.HelvetiCraft.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatusCommand implements CommandExecutor {

    private final Main plugin;

    public StatusCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Permission check
        if (!sender.hasPermission("helveticraft.status")) {
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        sender.sendMessage("§6--- §bHelvetiCraft Projektstatus §6---");

        // Projektphase und nächstes Update
        String stage = plugin.getConfig().getString("PROJECT_STAGE", "Unbekannt");
        String nextUpdate = plugin.getConfig().getString("NEXT_UPDATE", "Noch nicht festgelegt");

        sender.sendMessage("§aProjektphase: §e" + stage);
        sender.sendMessage("§aNächstes Update: §e" + nextUpdate);

        // Online-Spieler
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        sender.sendMessage("§aServer Online: §e" + online + "/" + max);

        // Volksinitiativen-Infos
        int totalInitiatives = plugin.getInitiativeManager().getTotalInitiatives();
        int totalVotes = plugin.getInitiativeManager().getTotalVotes();
        sender.sendMessage("§aGesamtanzahl Volksinitiativen: §e" + totalInitiatives);
        sender.sendMessage("§aGesamtanzahl Stimmen: §e" + totalVotes);

        sender.sendMessage("§7Diese Informationen sind relevant für die IDPA-Projektarbeit.");
        return true;
    }
}
