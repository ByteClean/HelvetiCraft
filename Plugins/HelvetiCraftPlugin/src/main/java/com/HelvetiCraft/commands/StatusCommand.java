package com.HelvetiCraft.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatusCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        // Permission check
        if (!player.hasPermission("helveticraft.status")) {
            player.sendMessage("§cYou don’t have permission to use this command.");
            return true;
        }

        player.sendMessage("§aYour current status:");
        player.sendMessage(" §7Name: §b" + player.getName());
        player.sendMessage(" §7Health: §c" + player.getHealth());
        player.sendMessage(" §7World: §e" + player.getWorld().getName());
        return true;
    }
}
