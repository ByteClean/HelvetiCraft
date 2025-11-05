package com.HelvetiCraft.commands;

import com.HelvetiCraft.Main;
import com.HelvetiCraft.requests.AdminRequests;
import com.HelvetiCraft.util.SafeScheduler;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * /admin <reason> - upgrades an 'admin' group member to 'super-admin' for 24 hours.
 * Running /admin again while 'super-admin' will downgrade immediately.
 */
public class AdminCommand implements CommandExecutor {

    private final Main plugin;
    private final Permission perms;
    private final Map<UUID, BukkitTask> scheduledDowngrades = new HashMap<>();
    private final Map<UUID, Long> expiryTimes = new HashMap<>();

    public AdminCommand(Main plugin) {
        this.plugin = plugin;

        RegisteredServiceProvider<Permission> rsp = Bukkit.getServicesManager().getRegistration(Permission.class);
        this.perms = rsp != null ? rsp.getProvider() : null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can run this command.");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        if (perms == null) {
            player.sendMessage("Permission provider not available (Vault required).");
            return true;
        }

        // If player is already in super-admin, this will downgrade them back to admin
        if (perms.playerInGroup(player, "super-admin")) {
            // downgrade
            boolean removed = perms.playerRemoveGroup((String) null, name, "super-admin");
            boolean added = perms.playerAddGroup((String) null, name, "admin");
            BukkitTask task = scheduledDowngrades.remove(uuid);
            if (task != null) task.cancel();
            expiryTimes.remove(uuid);
            AdminRequests.logDowngrade(uuid, name, "manual_downgrade");
            player.sendMessage("You have been downgraded to admin.");
            return true;
        }

        // Check that player is in admin group (requires Vault to query group)
        if (!perms.playerInGroup(player, "admin")) {
            player.sendMessage("You must be in the 'admin' group to use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("Usage: /admin <reason_for_upgrade>");
            return true;
        }

        String reason = String.join(" ", args);

        // perform upgrade: remove admin (optional) and add super-admin
    perms.playerRemoveGroup((String) null, name, "admin");
    perms.playerAddGroup((String) null, name, "super-admin");

        // schedule downgrade after 24 hours
        long expiresAt = System.currentTimeMillis() + 24L * 60L * 60L * 1000L;

        BukkitTask task = SafeScheduler.runLater(plugin, () -> {
            // downgrade
            perms.playerRemoveGroup((String) null, name, "super-admin");
            perms.playerAddGroup((String) null, name, "admin");
            scheduledDowngrades.remove(uuid);
            expiryTimes.remove(uuid);
            AdminRequests.logDowngrade(uuid, name, "auto_expired");
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null && p.isOnline()) p.sendMessage("Your super-admin role has expired and you were downgraded to admin.");
        }, 24L * 60L * 60L * 20L); // 24 hours in ticks

        scheduledDowngrades.put(uuid, task);
        expiryTimes.put(uuid, expiresAt);

        AdminRequests.logUpgrade(uuid, name, reason, expiresAt);
        player.sendMessage("You have been upgraded to super-admin for 24 hours. Reason: " + reason);
        return true;
    }
}
