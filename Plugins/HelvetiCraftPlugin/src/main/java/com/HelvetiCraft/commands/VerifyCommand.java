package com.HelvetiCraft.commands;

import com.HelvetiCraft.Main;
import com.HelvetiCraft.requests.VerifyRequests;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class VerifyCommand implements CommandExecutor {

    private final Main plugin;

    public VerifyCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl ausführen.");
            return true;
        }

        if (!player.hasPermission("helveticraft.verify")) {
            player.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        UUID uuid = player.getUniqueId();

        // Only generate code
        String code = VerifyRequests.generateCode(uuid);
        player.sendMessage("§aDein Verifizierungscode lautet: §e" + code);
        player.sendMessage("§7Gehe zum Discord-Bot und gib §b/verify " + code + " §7ein, um dein Konto zu verifizieren.");

        plugin.getLogger().info("Verifizierungscode für " + player.getName() + " generiert: " + code);
        return true;
    }

    // This method can be called later by the plugin when the backend confirms verification
    public void notifyVerified(Player player) {
        player.sendMessage("§aDein Minecraft-Konto wurde erfolgreich mit Discord verifiziert!");
        plugin.getLogger().info(player.getName() + " wurde erfolgreich verifiziert.");
    }
}
