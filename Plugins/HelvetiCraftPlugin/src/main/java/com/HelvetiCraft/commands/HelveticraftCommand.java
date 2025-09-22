package com.HelvetiCraft.commands;

import com.helveticraft.helveticraftplugin.Main;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HelveticraftCommand implements CommandExecutor {

    private final Main plugin;

    public HelveticraftCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Permission check
        if (!sender.hasPermission("helveticraft.help")) {
            sender.sendMessage("§cYou don’t have permission to use this command.");
            return true;
        }

        sender.sendMessage("§6====== §bHelvetiCraft Server Info §6======");
        sender.sendMessage("§7A fun and engaging Minecraft server for the community.");
        sender.sendMessage("§7Check out the server resources and connect with us:");

        if (sender instanceof Player player) {
            String discordUrl = plugin.getConfig().getString("DISCORD_URL", "https://discord.gg/placeholder");
            String websiteUrl = plugin.getConfig().getString("WEBSITE_URL", "https://example.com");

            TextComponent discordLink = new TextComponent("§bDiscord");
            discordLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, discordUrl));

            TextComponent websiteLink = new TextComponent("§bWebsite");
            websiteLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, websiteUrl));

            player.spigot().sendMessage(discordLink);
            player.spigot().sendMessage(websiteLink);
        } else {
            sender.sendMessage("§7Discord: " + plugin.getConfig().getString("DISCORD_URL"));
            sender.sendMessage("§7Website: " + plugin.getConfig().getString("WEBSITE_URL"));
        }

        sender.sendMessage("§e/initiative §7- Open initiatives menu");
        sender.sendMessage("§e/status §7- Check your status");
        sender.sendMessage("§e/verify §7- Verify your account");

        return true;
    }
}
