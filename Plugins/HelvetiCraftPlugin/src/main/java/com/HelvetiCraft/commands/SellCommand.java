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

    // Pending offers: buyerId -> list of offers
    private final Map<UUID, List<Offer>> pending = new ConcurrentHashMap<>();

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

        // Cleanup task for expired offers (Folia + Paper safe)
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                plugin,
                task -> {
                    long now = System.currentTimeMillis();
                    for (List<Offer> list : pending.values()) {
                        list.removeIf(o -> o.expiresAt < now);
                    }
                    pending.entrySet().removeIf(e -> e.getValue().isEmpty());
                },
                20L,       // initial delay (1 second)
                20L * 10   // repeat every 10 seconds
        );
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
                return handleAccept(player, args);
            case "selldecline":
                return handleDecline(player, args);
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

        // Check if seller already has an active offer
        boolean hasOffer = pending.values().stream()
                .flatMap(List::stream)
                .anyMatch(o -> o.sellerId.equals(seller.getUniqueId()) && o.expiresAt > System.currentTimeMillis());

        if (hasOffer) {
            seller.sendMessage("§cDu hast bereits ein aktives Angebot laufen.");
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
        long expiresAt = System.currentTimeMillis() + 120_000L; // 2 minutes
        Offer offer = new Offer(seller.getUniqueId(), buyer.getUniqueId(), offered, cents, expiresAt);
        pending.computeIfAbsent(buyer.getUniqueId(), k -> new ArrayList<>()).add(offer);

        String priceStr = FinanceManager.formatCents(cents);
        seller.sendMessage("§aAngebot gesendet an §f" + buyer.getName() + " §afür §f" + priceStr + "§a. Läuft in 2 Minuten ab.");

        // Clickable message
        TextComponent base = new TextComponent("§e" + seller.getName() + " bietet dir " + offered.getAmount() + "x "
                + prettify(offered.getType()) + " für §a" + priceStr + "§e an. ");
        TextComponent accept = new TextComponent("§a[Annehmen]");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sellaccept " + seller.getName()));
        TextComponent decline = new TextComponent(" §c[Ablehnen]");
        decline.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/selldecline " + seller.getName()));
        buyer.spigot().sendMessage(base, accept, decline);

        return true;
    }

    private boolean handleAccept(Player buyer, String[] args) {
        List<Offer> offers = pending.get(buyer.getUniqueId());
        if (offers == null || offers.isEmpty()) {
            buyer.sendMessage("§cKeine ausstehenden Angebote.");
            return true;
        }

        Offer o;
        if (args.length >= 1) {
            Player seller = Bukkit.getPlayerExact(args[0]);
            if (seller == null) {
                buyer.sendMessage("§cVerkäufer nicht gefunden.");
                return true;
            }
            o = offers.stream().filter(of -> of.sellerId.equals(seller.getUniqueId())).findFirst().orElse(null);
            if (o == null) {
                buyer.sendMessage("§cKein Angebot von diesem Verkäufer gefunden.");
                return true;
            }
            offers.remove(o);
        } else {
            o = offers.remove(0); // oldest
        }

        if (offers.isEmpty()) pending.remove(buyer.getUniqueId());

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

    private boolean handleDecline(Player buyer, String[] args) {
        List<Offer> offers = pending.get(buyer.getUniqueId());
        if (offers == null || offers.isEmpty()) {
            buyer.sendMessage("§cKeine ausstehenden Angebote.");
            return true;
        }

        Offer o;
        if (args.length >= 1) {
            Player seller = Bukkit.getPlayerExact(args[0]);
            if (seller == null) {
                buyer.sendMessage("§cVerkäufer nicht gefunden.");
                return true;
            }
            o = offers.stream().filter(of -> of.sellerId.equals(seller.getUniqueId())).findFirst().orElse(null);
            if (o == null) {
                buyer.sendMessage("§cKein Angebot von diesem Verkäufer gefunden.");
                return true;
            }
            offers.remove(o);
        } else {
            o = offers.remove(0); // oldest
        }

        if (offers.isEmpty()) pending.remove(buyer.getUniqueId());

        Player seller = Bukkit.getPlayer(o.sellerId);
        if (seller != null && seller.isOnline()) {
            seller.sendMessage("§e" + buyer.getName() + " §chat dein Angebot abgelehnt.");
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

        if (!(sender instanceof Player)) return res;

        Player player = (Player) sender;

        switch (cmd.getName().toLowerCase()) {
            case "sell":
                if (args.length == 1) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (!p.equals(player) && p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                            res.add(p.getName());
                        }
                    }
                }
                break;
            case "sellaccept":
            case "selldecline":
                if (args.length == 1) {
                    List<Offer> offers = pending.get(player.getUniqueId());
                    if (offers != null) {
                        for (Offer o : offers) {
                            Player seller = Bukkit.getPlayer(o.sellerId);
                            if (seller != null && seller.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                                res.add(seller.getName());
                            }
                        }
                    }
                }
                break;
        }

        return res;
    }
}
