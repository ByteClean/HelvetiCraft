package com.HelvetiCraft.commands;

import com.HelvetiCraft.finance.FinanceManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SellCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final FinanceManager finance;

    // Pending offers buyerId -> Offer
    private final Map<UUID, Offer> pending = new ConcurrentHashMap<>();

    private static class Offer {
        final UUID sellerId;
        final UUID buyerId;
        final ItemStack item;
        final long priceCents;
        final long expiresAt; // millis
        Offer(UUID s, UUID b, ItemStack i, long price, long expiresAt) {
            this.sellerId = s;
            this.buyerId = b;
            this.item = i;
            this.priceCents = price;
            this.expiresAt = expiresAt;
        }
    }

    public SellCommand(JavaPlugin plugin, FinanceManager finance) {
        this.plugin = plugin;
        this.finance = finance;

        // Cleanup task for expired offers
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                pending.values().removeIf(o -> o.expiresAt < now);
            }
        }.runTaskTimer(plugin, 20L, 20L * 10); // every 10 seconds
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur Spieler.");
            return true;
        }
        if (!sender.hasPermission("helveticraft.sell")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }

        switch (cmd.getName().toLowerCase()) {
            case "sell":
                return handleSell(player, args);
            case "sellaccept":
                return handleAccept(player);
            case "selldecline":
                return handleDecline(player);
        }
        return false;
    }

    private boolean handleSell(Player seller, String[] args) {
        if (args.length < 2) {
            seller.sendMessage("§eVerwendung: /sell <Spieler> <Betrag>");
            return true;
        }

        Player buyer = Bukkit.getPlayerExact(args[0]);
        if (buyer == null) {
            seller.sendMessage("§cKäufer nicht gefunden oder offline.");
            return true;
        }
        if (buyer.getUniqueId().equals(seller.getUniqueId())) {
            seller.sendMessage("§cDu kannst nicht an dich selbst verkaufen.");
            return true;
        }

        long cents;
        try {
            cents = FinanceManager.parseAmountToCents(args[1]);
        } catch (IllegalArgumentException ex) {
            seller.sendMessage("§cUngültiger Betrag. Beispiel: 12.34");
            return true;
        }
        if (cents <= 0) {
            seller.sendMessage("§cBetrag muss größer als 0 sein.");
            return true;
        }

        ItemStack inHand = seller.getInventory().getItemInMainHand();
        if (inHand == null || inHand.getType() == Material.AIR || inHand.getAmount() <= 0) {
            seller.sendMessage("§cDu hältst keinen gültigen Gegenstand in der Hand.");
            return true;
        }

        ItemStack offered = inHand.clone();
        long expiresAt = System.currentTimeMillis() + 30_000L;
        Offer offer = new Offer(seller.getUniqueId(), buyer.getUniqueId(), offered, cents, expiresAt);
        pending.put(buyer.getUniqueId(), offer);

        String priceStr = FinanceManager.formatCents(cents);
        seller.sendMessage("§aAngebot gesendet an §f" + buyer.getName() + " §afür §f" + priceStr + "§a. Läuft in 30s ab.");

        // Clickable message
        TextComponent base = new TextComponent("§e" + seller.getName() + " bietet dir " + offered.getAmount() + "x "
                + prettify(offered.getType()) + " für §a" + priceStr + "§e an. ");
        TextComponent accept = new TextComponent("§a[Annehmen]");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sellaccept"));
        TextComponent decline = new TextComponent(" §c[Ablehnen]");
        decline.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/selldecline"));
        buyer.spigot().sendMessage(base, accept, decline);

        return true;
    }

    private boolean handleAccept(Player buyer) {
        Offer o = pending.remove(buyer.getUniqueId());
        if (o == null) {
            buyer.sendMessage("§cKein ausstehendes Angebot.");
            return true;
        }
        if (System.currentTimeMillis() > o.expiresAt) {
            buyer.sendMessage("§cAngebot ist abgelaufen.");
            return true;
        }

        Player seller = Bukkit.getPlayer(o.sellerId);
        if (seller == null || !seller.isOnline()) {
            buyer.sendMessage("§cVerkäufer ist nicht mehr online.");
            return true;
        }

        if (finance.getMain(buyer.getUniqueId()) < o.priceCents) {
            buyer.sendMessage("§cUnzureichender Kontostand.");
            seller.sendMessage("§cKäufer hat nicht genug Geld.");
            return true;
        }

        if (buyer.getInventory().firstEmpty() == -1) {
            buyer.sendMessage("§cKein freier Inventarplatz.");
            seller.sendMessage("§cKäufer hat keinen Inventarplatz.");
            return true;
        }

        ItemStack current = seller.getInventory().getItemInMainHand();
        if (current == null || current.getType() != o.item.getType() || current.getAmount() < o.item.getAmount() || !current.isSimilar(o.item)) {
            buyer.sendMessage("§cAngebot ungültig: Verkäufer hält den Gegenstand nicht mehr.");
            seller.sendMessage("§cVerkauf fehlgeschlagen: Du hältst den angebotenen Gegenstand nicht mehr.");
            return true;
        }

        boolean paid = finance.transferMain(buyer.getUniqueId(), seller.getUniqueId(), o.priceCents);
        if (!paid) {
            buyer.sendMessage("§cZahlung fehlgeschlagen.");
            seller.sendMessage("§cZahlung fehlgeschlagen.");
            return true;
        }

        current.setAmount(current.getAmount() - o.item.getAmount());
        seller.getInventory().setItemInMainHand(current.getAmount() <= 0 ? null : current);
        buyer.getInventory().addItem(o.item);

        String priceStr = FinanceManager.formatCents(o.priceCents);
        buyer.sendMessage("§aGekauft: §f" + o.item.getAmount() + "x " + prettify(o.item.getType()) + " §afür §f" + priceStr + " §avon §f" + seller.getName());
        seller.sendMessage("§aVerkauft: §f" + o.item.getAmount() + "x " + prettify(o.item.getType()) + " §aan §f" + buyer.getName() + " §afür §f" + priceStr);

        finance.save();
        return true;
    }

    private boolean handleDecline(Player buyer) {
        Offer o = pending.remove(buyer.getUniqueId());
        if (o == null) {
            buyer.sendMessage("§cKein ausstehendes Angebot.");
            return true;
        }

        Player seller = Bukkit.getPlayer(o.sellerId);
        if (seller != null && seller.isOnline()) {
            seller.sendMessage("§e" + buyer.getName() + " §chat das Angebot abgelehnt.");
        }
        buyer.sendMessage("§eAngebot abgelehnt.");
        return true;
    }

    private String prettify(Material m) {
        String n = m.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> res = new ArrayList<>();
        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (sender.getName().equalsIgnoreCase(p.getName())) continue;
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    res.add(p.getName());
                }
            }
        }
        return res;
    }
}
