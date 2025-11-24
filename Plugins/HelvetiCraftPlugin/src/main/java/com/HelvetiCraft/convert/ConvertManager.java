package com.HelvetiCraft.convert;

import com.HelvetiCraft.Main;
import com.HelvetiCraft.finance.FinanceManager;
import com.HelvetiCraft.requests.TaxRequests;
import com.HelvetiCraft.util.FinanceTransactionLogger;
import com.HelvetiCraft.util.SafeScheduler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ConvertManager implements Listener {

    private final Main plugin;
    private final FinanceManager finance;
    private final ConvertMenu menu;
    private final Set<UUID> openMenus = new HashSet<>();

    public ConvertManager(Main plugin, FinanceManager finance) {
        this.plugin = plugin;
        this.finance = finance;
        this.menu = new ConvertMenu(this, finance);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openConvertMenu(Player player) {
        UUID uuid = player.getUniqueId();
        if (openMenus.contains(uuid)) {
            player.sendMessage("§cDu hast bereits ein Konvertierungsfenster offen.");
            return;
        }
        menu.open(player);
        openMenus.add(uuid);
    }

    private void unregisterPlayer(UUID uuid) {
        openMenus.remove(uuid);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getView() == null) return;
        if (!"§6§lErz → CHF Konvertierung".equals(e.getView().getTitle())) return;

        UUID uuid = p.getUniqueId();
        if (!openMenus.contains(uuid)) return;

        int slot = e.getRawSlot();
        ItemStack current = e.getCurrentItem();

        if (slot == 26 && current != null && current.getType() == Material.GREEN_WOOL) {
            e.setCancelled(true);
            processSale(p);
            return;
        }

        if (e.getClick() == ClickType.DOUBLE_CLICK) {
            e.setCancelled(true);
        }

        // Folia-safe: update sell button 1 tick later
        SafeScheduler.runLater(plugin, () -> menu.updateSellButton(p), 1L);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!"§6§lErz → CHF Konvertierung".equals(e.getView().getTitle())) return;

        // Folia-safe: update sell button 1 tick later
        SafeScheduler.runLater(plugin, () -> menu.updateSellButton(p), 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        UUID uuid = p.getUniqueId();

        if (!"§6§lErz → CHF Konvertierung".equals(e.getView().getTitle())) return;
        if (!openMenus.contains(uuid)) return;

        Inventory top = e.getView().getTopInventory();
        returnItems(p, top);
        unregisterPlayer(uuid);
    }

    private void processSale(Player p) {
        Inventory inv = p.getOpenInventory().getTopInventory();
        List<ItemStack> toRemove = new ArrayList<>();
        long total = 0;

        for (ItemStack item : inv.getContents()) {
            if (item == null || item.getType() == Material.GREEN_WOOL || !isConvertible(item.getType())) continue;
            long rate = getRate(item.getType());
            total += rate * item.getAmount();
            toRemove.add(item);
        }

        if (total == 0) {
            p.sendMessage("§cKeine konvertierbaren Erze im Fenster.");
            return;
        }

        long tax = TaxRequests.getOreConvertTax();
        long finalTotal = total - tax;
        UUID govUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

        // Remove ores from GUI
        for (ItemStack item : toRemove) inv.removeItem(item);

        // Give player full total
        FinanceTransactionLogger logger = new FinanceTransactionLogger(finance);
        logger.logTransaction("conversion", null, p.getUniqueId(), finalTotal);

        // Give tax to government account
        logger.logTransaction("conversion-tax", null, govUUID, tax);

        p.sendMessage("§a§lKonvertierung erfolgreich!");
        p.sendMessage("§7Erlös: §a" + FinanceManager.formatCents(total) + " CHF");
        p.sendMessage("§7Gebühr: §c" + FinanceManager.formatCents(tax) + " CHF");
        p.sendMessage("§7= §6§l" + FinanceManager.formatCents(finalTotal) + " CHF §7gutgeschrieben.");

        menu.updateSellButton(p);
    }

    private void returnItems(Player p, Inventory inv) {
        for (ItemStack item : inv.getContents()) {
            if (item == null || item.getType() == Material.GREEN_WOOL) continue;
            HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(item.clone());
            leftover.values().forEach(drop -> p.getWorld().dropItemNaturally(p.getLocation(), drop));
        }
    }

    public boolean isConvertible(Material m) {
        return switch (m) {
            case COAL, COAL_ORE, DEEPSLATE_COAL_ORE,
                 RAW_IRON, IRON_INGOT, IRON_ORE, DEEPSLATE_IRON_ORE,
                 RAW_COPPER, COPPER_INGOT, COPPER_ORE, DEEPSLATE_COPPER_ORE,
                 RAW_GOLD, GOLD_INGOT, GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE,
                 REDSTONE, REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE,
                 LAPIS_LAZULI, LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                 DIAMOND, DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                 EMERALD, EMERALD_ORE, DEEPSLATE_EMERALD_ORE,
                 QUARTZ, NETHER_QUARTZ_ORE,
                 ANCIENT_DEBRIS, NETHERITE_SCRAP -> true;
            default -> false;
        };
    }

    public long getRate(Material m) {
        return switch (m) {
            case COAL, COAL_ORE, DEEPSLATE_COAL_ORE -> TaxRequests.COAL_ORE_CONVERSION;
            case RAW_IRON, IRON_INGOT, IRON_ORE, DEEPSLATE_IRON_ORE -> TaxRequests.IRON_ORE_CONVERSION;
            case RAW_COPPER, COPPER_INGOT, COPPER_ORE, DEEPSLATE_COPPER_ORE -> TaxRequests.COPPER_ORE_CONVERSION;
            case RAW_GOLD, GOLD_INGOT, GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE -> TaxRequests.GOLD_ORE_CONVERSION;
            case REDSTONE, REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> TaxRequests.REDSTONE_ORE_CONVERSION;
            case LAPIS_LAZULI, LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> TaxRequests.LAPIS_ORE_CONVERSION;
            case DIAMOND, DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> TaxRequests.DIAMOND_ORE_CONVERSION;
            case EMERALD, EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> TaxRequests.EMERALD_ORE_CONVERSION;
            case QUARTZ, NETHER_QUARTZ_ORE -> TaxRequests.QUARTZ_ORE_CONVERSION;
            case ANCIENT_DEBRIS, NETHERITE_SCRAP -> TaxRequests.ANCIENT_DEBRIS_CONVERSION;
            default -> 0;
        };
    }
}
