package com.HelvetiCraft.commands;

import com.HelvetiCraft.convert.ConvertManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static com.HelvetiCraft.requests.TaxRequests.loadAllTaxConfigFromBackend;

public class ConvertCommand implements CommandExecutor {

    private final ConvertManager convertManager;

    public ConvertCommand(ConvertManager convertManager) {
        this.convertManager = convertManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl verwenden.");
            return true;
        }
        if (!p.hasPermission("helveticraft.convert")) {
            p.sendMessage("§cKeine Berechtigung.");
            return true;
        }

        loadAllTaxConfigFromBackend();
        convertManager.openConvertMenu(p);
        return true;
    }
}