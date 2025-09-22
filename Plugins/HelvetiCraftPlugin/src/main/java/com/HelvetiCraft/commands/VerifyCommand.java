package com.HelvetiCraft.commands;

import com.helveticraft.helveticraftplugin.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VerifyCommand implements CommandExecutor {

    private final Main plugin;

    // Store temporary verification codes: MC UUID -> code
    private final Map<UUID, String> pendingCodes = new HashMap<>();

    public VerifyCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("helveticraft.verify")) {
            player.sendMessage("§cYou don’t have permission to use this command.");
            return true;
        }

        // If no argument: generate code for the player
        if (args.length == 0) {
            // TODO: Replace this with real backend request
            String code = generateDummyCode(player.getUniqueId());
            player.sendMessage("§aYour verification code is: §e" + code);
            player.sendMessage("§7Go to the Discord bot and type §b/verify " + code + " §7to verify your account.");
            return true;
        }

        // If the player provides a code: attempt verification
        String inputCode = args[0];
        String expectedCode = pendingCodes.get(player.getUniqueId());

        if (expectedCode == null) {
            player.sendMessage("§cYou don’t have a pending verification. Run §e/verify §cto get a code first.");
            return true;
        }

        if (inputCode.equalsIgnoreCase(expectedCode)) {
            player.sendMessage("§aYour Minecraft account has been successfully verified with Discord!");
            pendingCodes.remove(player.getUniqueId());
            plugin.getLogger().info(player.getName() + " has been verified (dummy).");
        } else {
            player.sendMessage("§cIncorrect verification code. Please check the code from Discord.");
        }

        return true;
    }

    private String generateDummyCode(UUID playerUUID) {
        // Generate a simple 6-character alphanumeric code
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int idx = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(idx));
        }
        String code = sb.toString();
        pendingCodes.put(playerUUID, code);
        plugin.getLogger().info("Generated dummy verification code " + code + " for UUID " + playerUUID);
        return code;
    }
}
