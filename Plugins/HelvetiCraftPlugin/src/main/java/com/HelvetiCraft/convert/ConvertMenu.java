package com.HelvetiCraft.convert;

import com.HelvetiCraft.finance.FinanceManager;
import com.HelvetiCraft.requests.TaxRequests;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ConvertMenu {

    private final ConvertManager manager;
    private final FinanceManager finance;

    public ConvertMenu(ConvertManager manager, FinanceManager finance) {
        this.manager = manager;
        this.finance = finance;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lErz → CHF Konvertierung");

        ItemStack sell = createItem(
                Material.GREEN_WOOL,
                "§a§lVerkaufen",
                "§7Lege Erze in die Truhe und klicke hier",
                "§7Gebühr: §c" + FinanceManager.formatCents(TaxRequests.getOreConvertTax()) + " CHF"
        );
        inv.setItem(26, sell);

        player.openInventory(inv);
        updateSellButton(player);
    }

    public void updateSellButton(Player player) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (inv == null || !inv.getViewers().contains(player)) return;

        ItemStack sell = inv.getItem(26);
        if (sell == null || sell.getType() != Material.GREEN_WOOL) return;

        long total = 0;
        for (ItemStack item : inv.getContents()) {
            if (item == null || item.getType() == Material.GREEN_WOOL || !manager.isConvertible(item.getType())) continue;
            total += manager.getRate(item.getType()) * item.getAmount();
        }

        ItemMeta meta = sell.getItemMeta();
        if (meta == null) return;

        List<String> lore = new ArrayList<>();
        lore.add("§7Klicke, um alle Erze zu verkaufen");

        long tax = TaxRequests.getOreConvertTax();
        long finalTotal = total - tax;
        if (total > 0) {
            lore.add("§7Erlös: §a" + FinanceManager.formatCents(total) + " CHF");
            lore.add("§7Gebühr: §c" + FinanceManager.formatCents(tax) + " CHF");
            lore.add("§7= §6" + FinanceManager.formatCents(finalTotal) + " CHF §7gutgeschrieben");
        } else {
            lore.add("§7Lege Erze in die Truhe");
            lore.add("§7Gebühr: §c" + FinanceManager.formatCents(tax) + " CHF");
        }

        meta.setLore(lore);
        sell.setItemMeta(meta);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
