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
            sender.sendMessage("§cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
            return true;
        }

        sender.sendMessage("§6====== §bHelvetiCraft Server Info §6======");
        sender.sendMessage("§7Ein unterhaltsamer und gemeinschaftlicher Minecraft-Server.");
        sender.sendMessage("§7Hier findest du Ressourcen und Kontaktmöglichkeiten:");

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

        sender.sendMessage("§e/initiative §7- Öffne das Volksinitiativen-Menü");
        sender.sendMessage("§e/status §7- Überprüfe deinen Status");
        sender.sendMessage("§e/verify §7- Verifiziere deinen Discord Account");
        sender.sendMessage("§e/finance §7- Finanzübersicht anzeigen");
        sender.sendMessage("§e/networth §7- Zeige dein Vermögen an");
        sender.sendMessage("§e/pay <Spieler> <Betrag> §7- Überweise Geld an einen Spieler");
        sender.sendMessage("§e/sell <Anzahl> §7- Verkaufe Items aus deiner Hand");
        sender.sendMessage("§e/save <Betrag> <main|savings> §7- Verschiebe Geld zwischen Haupt- und Sparkonto");
        sender.sendMessage("§6=====================================");

        return true;
    }
}
