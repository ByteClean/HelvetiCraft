package com.HelvetiCraft.commands;

import com.HelvetiCraft.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VerifyCommand implements CommandExecutor {

    private final Main plugin;

    // Temporäre Verifizierungscodes speichern: MC UUID -> Code
    private final Map<UUID, String> pendingCodes = new HashMap<>();

    public VerifyCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl ausführen.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("helveticraft.verify")) {
            player.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        // Kein Argument: generiere Code für den Spieler
        if (args.length == 0) {
            // TODO: Mit echtem Backend ersetzen
            String code = generateDummyCode(player.getUniqueId());
            player.sendMessage("§aDein Verifizierungscode lautet: §e" + code);
            player.sendMessage("§7Gehe zum Discord-Bot und gib §b/verify " + code + " §7ein, um dein Konto zu verifizieren.");
            return true;
        }

        // Spieler gibt einen Code ein: versuche Verifizierung
        String inputCode = args[0];
        String expectedCode = pendingCodes.get(player.getUniqueId());

        if (expectedCode == null) {
            player.sendMessage("§cDu hast keine ausstehende Verifizierung. Nutze §e/verify §c, um zuerst einen Code zu erhalten.");
            return true;
        }

        if (inputCode.equalsIgnoreCase(expectedCode)) {
            player.sendMessage("§aDein Minecraft-Konto wurde erfolgreich mit Discord verifiziert!");
            pendingCodes.remove(player.getUniqueId());
            plugin.getLogger().info(player.getName() + " wurde erfolgreich verifiziert (Dummy).");
        } else {
            player.sendMessage("§cFalscher Verifizierungscode. Bitte überprüfe den Code aus Discord.");
        }

        return true;
    }

    private String generateDummyCode(UUID playerUUID) {
        // Einfachen 6-stelligen alphanumerischen Code generieren
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int idx = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(idx));
        }
        String code = sb.toString();
        pendingCodes.put(playerUUID, code);
        plugin.getLogger().info("Generierter Dummy-Verifizierungscode " + code + " für UUID " + playerUUID);
        return code;
    }
}
