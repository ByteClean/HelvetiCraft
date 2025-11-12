package com.HelvetiCraft.commands;

import com.HelvetiCraft.Claims.LandTaxManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class LandTaxCommand implements CommandExecutor {

    private final LandTaxManager manager;

    public LandTaxCommand(LandTaxManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("§a[LandTax] Berechne Landsteuer, bitte warten...");
        manager.collectLandTax();
        return true;
    }
}
