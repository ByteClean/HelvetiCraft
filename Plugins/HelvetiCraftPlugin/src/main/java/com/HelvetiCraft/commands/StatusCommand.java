package com.HelvetiCraft.commands;

import com.helveticraft.helveticraftplugin.Main;
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
            sender.sendMessage("§cYou don’t have permission to use this command.");
            return true;
        }

        sender.sendMessage("§6--- §bHelvetiCraft Project Status §6---");

        // Project stage and next update
        String stage = plugin.getConfig().getString("PROJECT_STAGE", "Unknown");
        String nextUpdate = plugin.getConfig().getString("NEXT_UPDATE", "TBD");

        sender.sendMessage("§aStage: §e" + stage);
        sender.sendMessage("§aNext Update: §e" + nextUpdate);

        // Online players
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        sender.sendMessage("§aServer Online: §e" + online + "/" + max);

        // Initiatives info
        int totalInitiatives = plugin.getInitiativeManager().getTotalInitiatives();
        int totalVotes = plugin.getInitiativeManager().getTotalVotes();
        sender.sendMessage("§aTotal Initiatives: §e" + totalInitiatives);
        sender.sendMessage("§aTotal Votes: §e" + totalVotes);

        sender.sendMessage("§7This information is relevant for the IDPA project work.");
        return true;
    }
}
