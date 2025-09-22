package com.HelvetiCraft.initiatives;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class InitiativeMenu {

    private final InitiativeManager manager;

    public InitiativeMenu(InitiativeManager manager) {
        this.manager = manager;
    }

    public void open(Player player, int page) {
        int initiativesPerPage = 18;
        List<Initiative> initiativeList = new ArrayList<>(manager.getInitiatives().values());

        int totalPages = (int) Math.ceil((double) initiativeList.size() / initiativesPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        // Save the page for the player
        manager.getPlayerPages().put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 27,
                "§6Active Initiatives (Page " + (page + 1) + "/" + totalPages + ")");

        Set<String> votedSet = manager.getPlayerVotes()
                .getOrDefault(player.getUniqueId(), new HashSet<>());

        // Fill initiatives for current page
        int startIndex = page * initiativesPerPage;
        int endIndex = Math.min(startIndex + initiativesPerPage, initiativeList.size());
        for (int i = startIndex; i < endIndex; i++) {
            Initiative initiative = initiativeList.get(i);
            boolean voted = votedSet.contains(initiative.getTitle());
            ItemStack item = new ItemStack(voted ? Material.GREEN_WOOL : Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName((voted ? "§a" : "§b") + initiative.getTitle());
                List<String> lore = new ArrayList<>();
                lore.add("§7Author: " + initiative.getAuthor());
                lore.add("§7Description: " + initiative.getDescription());
                lore.add("§eVotes: " + initiative.getVotes());
                if (voted) lore.add("§aYou already voted!");
                else lore.add("§7[Click to Vote]");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.addItem(item);
        }

        // Add control buttons
        addControls(inv, page, totalPages);

        player.openInventory(inv);
    }

    private void addControls(Inventory inv, int page, int totalPages) {
        // Create Initiative
        ItemStack createItem = new ItemStack(Material.EMERALD);
        ItemMeta createMeta = createItem.getItemMeta();
        if (createMeta != null) {
            createMeta.setDisplayName("§aCreate New Initiative");
            createMeta.setLore(Collections.singletonList("§7Click to create a new initiative"));
            createItem.setItemMeta(createMeta);
        }
        inv.setItem(18, createItem);

        // Prev Page
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName("§ePrevious Page");
                prev.setItemMeta(prevMeta);
            }
            inv.setItem(24, prev);
        }

        // Next Page
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§eNext Page");
                next.setItemMeta(nextMeta);
            }
            inv.setItem(25, next);
        }
    }
}
