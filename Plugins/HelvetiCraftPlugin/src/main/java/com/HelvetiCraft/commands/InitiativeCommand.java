package com.HelvetiCraft.commands;

import com.HelvetiCraft.initiatives.InitiativeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InitiativeCommand implements CommandExecutor {

    private final InitiativeManager initiativeManager;

    public InitiativeCommand(InitiativeManager initiativeManager) {
        this.initiativeManager = initiativeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("helveticraft.initiative")) {
            player.sendMessage("§cYou don’t have permission to use this command.");
            return true;
        }
        initiativeManager.openInitiativeMenu(player);
        return true;
    }
}
