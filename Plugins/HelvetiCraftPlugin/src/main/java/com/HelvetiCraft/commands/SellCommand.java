package com.HelvetiCraft.commands;

import com.HelvetiCraft.finance.FinanceManager;
import com.HelvetiCraft.requests.TaxRequests;
import com.HelvetiCraft.util.FinanceTransactionLogger;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SellCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final FinanceManager finance;

    private final Map<UUID, List<Offer>> pending = new ConcurrentHashMap<>();

    private static class Offer {
        final UUID sellerId;
        final UUID buyerId;
        final ItemStack item;
        final long priceCents;
        final long expiresAt;
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

        Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                plugin,
                task -> {
                    long now = System.currentTimeMillis();
                    for (List<Offer> list : pending.values()) {
                        list.removeIf(o -> o.expiresAt < now);
                    }
                    pending.entrySet().removeIf(e -> e.getValue().isEmpty());
                },
                20L, 20L * 10
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

        return switch (cmd.getName().toLowerCase()) {
            case "sell" -> handleSell(player, args);
            case "sellaccept" -> handleAccept(player, args);
            case "selldecline" -> handleDecline(player, args);
            default -> false;
        };
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
        if (buyer.equals(seller)) {
            seller.sendMessage("§cDu kannst nicht an dich selbst verkaufen.");
            return true;
        }

        if (pending.values().stream()
                .flatMap(List::stream)
                .anyMatch(o -> o.sellerId.equals(seller.getUniqueId()) && o.expiresAt > System.currentTimeMillis())) {
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
        long expiresAt = System.currentTimeMillis() + 120_000L;
        Offer offer = new Offer(seller.getUniqueId(), buyer.getUniqueId(), offered, cents, expiresAt);
        pending.computeIfAbsent(buyer.getUniqueId(), k -> new ArrayList<>()).add(offer);

        String priceStr = FinanceManager.formatCents(cents);
        seller.sendMessage("§aAngebot gesendet an §f" + buyer.getName() + " §afür §f" + priceStr + " CHF§a. Läuft in 2 Minuten ab.");

        // --- Käufer-Nachricht mit Steuerübersicht ---
        double taxRate = TaxRequests.getVerkaufsSteuer1zu1() / 100.0;
        long taxCents = (long) (cents * taxRate);
        long totalCents = cents + taxCents;

        String itemName = prettify(offered.getType());
        String priceCHF = FinanceManager.formatCents(cents) + " CHF";
        String taxCHF = FinanceManager.formatCents(taxCents) + " CHF";
        String totalCHF = FinanceManager.formatCents(totalCents) + " CHF";

        TextComponent msg = new TextComponent("§8§m          §r §6Verkaufsangebot §r §8§m          §r\n");
        msg.addExtra("§e" + seller.getName() + " §7verkauft dir:\n");
        msg.addExtra("§f " + offered.getAmount() + "× " + itemName + "\n\n");

        msg.addExtra("§7Preis:     §a" + priceCHF + "\n");
        msg.addExtra("§7+ Steuer:  §c" + taxCHF + " §8(26%)\n");
        msg.addExtra("§7§l= Gesamt:  §6§l" + totalCHF + "\n\n");

        TextComponent accept = new TextComponent("§a§l[✔ Akzeptieren]");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sellaccept " + seller.getName()));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§aKaufen für " + totalCHF)));

        TextComponent decline = new TextComponent("  §c§l[✖ Ablehnen]");
        decline.setClickEvent(new ClickEvent  (ClickEvent.Action.RUN_COMMAND, "/selldecline " + seller.getName()));
        decline.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§cAngebot ablehnen")));

        msg.addExtra(accept);
        msg.addExtra(decline);
        msg.addExtra("\n§8§m                                      §r");

        buyer.spigot().sendMessage(msg);

        return true;
    }

    private boolean handleAccept(Player buyer, String[] args) {
        List<Offer> offers = pending.get(buyer.getUniqueId());
        if (offers == null || offers.isEmpty()) {
            buyer.sendMessage("§cKeine ausstehenden Angebote.");
            return true;
        }

        Offer o = extractOffer(buyer, offers, args);
        if (o == null) return true;

        if (System.currentTimeMillis() > o.expiresAt) {
            buyer.sendMessage("§cAngebot ist abgelaufen.");
            return true;
        }

        Player seller = Bukkit.getPlayer(o.sellerId);
        if (seller == null || !seller.isOnline()) {
            buyer.sendMessage("§cVerkäufer ist nicht mehr online.");
            return true;
        }

        double taxRate = TaxRequests.getVerkaufsSteuer1zu1() / 100.0;
        long taxCents = (long) (o.priceCents * taxRate);
        long totalCost = o.priceCents + taxCents;

        if (finance.getMain(buyer.getUniqueId()) < totalCost) {
            buyer.sendMessage("§cUnzureichender Kontostand (benötigt: " + FinanceManager.formatCents(totalCost) + " CHF).");
            seller.sendMessage("§cKäufer hat nicht genug Geld.");
            return true;
        }

        if (buyer.getInventory().firstEmpty() == -1) {
            buyer.sendMessage("§cKein freier Inventarplatz.");
            seller.sendMessage("§cKäufer hat keinen Platz im Inventar.");
            return true;
        }

        ItemStack current = seller.getInventory().getItemInMainHand();
        if (current == null || !current.isSimilar(o.item) || current.getAmount() < o.item.getAmount()) {
            buyer.sendMessage("§cAngebot ungültig: Verkäufer hält den Gegenstand nicht mehr.");
            seller.sendMessage("§cVerkauf fehlgeschlagen: Du hältst den Gegenstand nicht mehr.");
            return true;
        }

        // Zahlung
        FinanceTransactionLogger logger = new FinanceTransactionLogger(finance);
        logger.logTransaction("1to1-sold", buyer.getUniqueId(), seller.getUniqueId(), o.priceCents);
        boolean paidNet = true;
        //boolean paidNet = finance.transferMain(buyer.getUniqueId(), seller.getUniqueId(), o.priceCents);
        logger.logTransaction("1to1-tax", buyer.getUniqueId(), com.HelvetiCraft.Claims.ClaimManager.GOVERNMENT_UUID, taxCents);
        boolean paidTax = true;
        //boolean paidTax = finance.transferMain(buyer.getUniqueId(), com.HelvetiCraft.Claims.ClaimManager.GOVERNMENT_UUID, taxCents);
        if (!paidNet || !paidTax) {
            buyer.sendMessage("§cZahlung fehlgeschlagen.");
            return true;
        }

        current.setAmount(current.getAmount() - o.item.getAmount());
        seller.getInventory().setItemInMainHand(current.getAmount() <= 0 ? null : current);
        buyer.getInventory().addItem(o.item.clone());

        String priceStr = FinanceManager.formatCents(o.priceCents) + " CHF";
        String taxStr = FinanceManager.formatCents(taxCents) + " CHF";
        String totalStr = FinanceManager.formatCents(totalCost) + " CHF";

        buyer.sendMessage("§a§lKauf erfolgreich! §f" + o.item.getAmount() + "× " + prettify(o.item.getType()) +
                " §avon §f" + seller.getName() + " §afür §a" + priceStr + " §7+ §c" + taxStr + " §7= §6" + totalStr);

        seller.sendMessage("§a§lVerkauf erfolgreich! §f" + o.item.getAmount() + "× " + prettify(o.item.getType()) +
                " §aan §f" + buyer.getName() + " §afür §a" + priceStr + " CHF §7(nach Steuer)");

        finance.save();
        return true;
    }

    private Offer extractOffer(Player buyer, List<Offer> offers, String[] args) {
        Offer o;
        if (args.length >= 1) {
            Player seller = Bukkit.getPlayerExact(args[0]);
            if (seller == null) {
                buyer.sendMessage("§cVerkäufer nicht gefunden.");
                return null;
            }
            o = offers.stream()
                    .filter(of -> of.sellerId.equals(seller.getUniqueId()))
                    .findFirst().orElse(null);
            if (o == null) {
                buyer.sendMessage("§cKein Angebot von diesem Verkäufer.");
                return null;
            }
            offers.remove(o);
        } else {
            o = offers.remove(0);
        }
        if (offers.isEmpty()) pending.remove(buyer.getUniqueId());
        return o;
    }

    private boolean handleDecline(Player buyer, String[] args) {
        List<Offer> offers = pending.get(buyer.getUniqueId());
        if (offers == null || offers.isEmpty()) {
            buyer.sendMessage("§cKeine ausstehenden Angebote.");
            return true;
        }

        Offer o = extractOffer(buyer, offers, args);
        if (o == null) return true;

        Player seller = Bukkit.getPlayer(o.sellerId);
        if (seller != null && seller.isOnline()) {
            seller.sendMessage("§e" + buyer.getName() + " §chat dein Angebot abgelehnt.");
        }
        buyer.sendMessage("§eAngebot von §f" + (seller != null ? seller.getName() : "Unbekannt") + " §eablehnt.");
        return true;
    }

    private String prettify(Material m) {
        String n = m.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> res = new ArrayList<>();
        if (!(sender instanceof Player player)) return res;

        return switch (cmd.getName().toLowerCase()) {
            case "sell" -> {
                if (args.length == 1) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (!p.equals(player) && p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                            res.add(p.getName());
                        }
                    }
                }
                yield res;
            }
            case "sellaccept", "selldecline" -> {
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
                yield res;
            }
            default -> res;
        };
    }
}