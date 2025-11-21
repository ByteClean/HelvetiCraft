package com.HelvetiCraft.commands;

import com.HelvetiCraft.taxes.LandTaxManager;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class LandTaxCommand implements CommandExecutor {

    private final LandTaxManager manager;
    private final Permission perms;

    public LandTaxCommand(LandTaxManager manager) {
        this.manager = manager;

        // Vault Permission Provider holen
        RegisteredServiceProvider<Permission> rsp =
                Bukkit.getServicesManager().getRegistration(Permission.class);

        this.perms = rsp != null ? rsp.getProvider() : null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Nur Spieler dürfen den Befehl ausführen
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Befehl kann nur von Spielern ausgeführt werden.");
            return true;
        }

        Player player = (Player) sender;

        if (perms == null) {
            player.sendMessage("§cFehler: Permissionsystem nicht verfügbar (Vault benötigt).");
            return true;
        }

        // === Permission prüfen: Spieler muss in Gruppe "super-admin" sein ===
        if (!perms.playerInGroup(player, "super-admin")) {
            player.sendMessage("§cDu benötigst die Gruppe §4super-admin§c, um diesen Befehl auszuführen.");
            return true;
        }

        // === Landsteuer starten ===
        player.sendMessage("§a[LandTax] Berechne Landsteuer... bitte warten.");
        manager.collectLandTax();

        return true;
    }
}
