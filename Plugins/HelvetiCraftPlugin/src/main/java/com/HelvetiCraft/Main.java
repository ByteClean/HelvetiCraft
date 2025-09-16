package com.helveticraft.helveticraftplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("HelvetiCraft Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("HelvetiCraft Plugin has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("verify")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players.");
                return true;
            }

            Player player = (Player) sender;

            // --- Dummy backend request (to be replaced later) ---
            // For now we just simulate a code coming back from the backend
            String uniqueCode = "ABC123"; // TODO: Call backend API to generate real code

            // Send private message to player
            player.sendMessage("§aYour verification code is: §e" + uniqueCode);
            player.sendMessage("§7Please go to our Discord and run §b/verify " + uniqueCode + " §7to link your account.");

            return true;
        }
        return false;
    }
}
