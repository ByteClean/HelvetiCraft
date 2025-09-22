package com.HelvetiCraft.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class HelveticraftCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Permission check
        if (!sender.hasPermission("helveticraft.help")) {
            sender.sendMessage("§cYou don’t have permission to use this command.");
            return true;
        }

        sender.sendMessage("§6====== §bHelvetiCraft Plugin §6======");
        sender.sendMessage("§e/initiative §7- Open initiatives menu");
        sender.sendMessage("§e/status §7- Check your status");
        sender.sendMessage("§e/verify §7- Verify your account");
        return true;
    }
}
