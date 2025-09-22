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

    // Tracks selected initiative per player in "own initiatives" menu
    private final Map<UUID, String> selectedInitiatives = new HashMap<>();

    public InitiativeMenu(InitiativeManager manager) {
        this.manager = manager;
    }

    // --- Main initiatives menu ---
    public void open(Player player, int page) {
        int initiativesPerPage = 18;
        List<Initiative> initiativeList = new ArrayList<>(manager.getInitiatives().values());

        int totalPages = (int) Math.ceil((double) initiativeList.size() / initiativesPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

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

        // Add control buttons (create + my initiatives)
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

        // My Initiatives Button
        ItemStack myInitiatives = new ItemStack(Material.BOOK);
        ItemMeta bookMeta = myInitiatives.getItemMeta();
        if (bookMeta != null) {
            bookMeta.setDisplayName("§eMy Initiatives");
            bookMeta.setLore(Collections.singletonList("§7View and manage your initiatives"));
            myInitiatives.setItemMeta(bookMeta);
        }
        inv.setItem(19, myInitiatives);

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

    // --- Player’s own initiatives menu ---
    public void openPlayerInitiatives(Player player, int page) {
        List<Initiative> ownList = new ArrayList<>();
        for (Initiative i : manager.getInitiatives().values()) {
            if (i.getAuthor().equals(player.getName())) ownList.add(i);
        }

        int initiativesPerPage = 18;
        int totalPages = (int) Math.ceil((double) ownList.size() / initiativesPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        manager.getPlayerPages().put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 27,
                "§6My Initiatives (Page " + (page + 1) + "/" + totalPages + ")");

        int startIndex = page * initiativesPerPage;
        int endIndex = Math.min(startIndex + initiativesPerPage, ownList.size());
        String selected = selectedInitiatives.get(player.getUniqueId());

        for (int i = startIndex; i < endIndex; i++) {
            Initiative initiative = ownList.get(i);
            Material mat = (initiative.getTitle().equals(selected)) ? Material.ENCHANTED_BOOK : Material.PAPER;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b" + initiative.getTitle());
                List<String> lore = new ArrayList<>();
                lore.add("§7Description: " + initiative.getDescription());
                lore.add("§eVotes: " + initiative.getVotes());
                if (initiative.getTitle().equals(selected)) lore.add("§aSelected!");
                else lore.add("§7[Click to Select]");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.addItem(item);
        }

        // Bottom row: back, edit, delete
        // Back
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§eBack to Initiatives");
            back.setItemMeta(backMeta);
        }
        inv.setItem(18, back);

        // Edit
        ItemStack edit = new ItemStack(Material.YELLOW_WOOL);
        ItemMeta editMeta = edit.getItemMeta();
        if (editMeta != null) {
            editMeta.setDisplayName("§6Edit Selected");
            edit.setItemMeta(editMeta);
        }
        inv.setItem(19, edit);

        // Delete
        ItemStack delete = new ItemStack(Material.RED_WOOL);
        ItemMeta deleteMeta = delete.getItemMeta();
        if (deleteMeta != null) {
            deleteMeta.setDisplayName("§cDelete Selected");
            delete.setItemMeta(deleteMeta);
        }
        inv.setItem(20, delete);

        player.openInventory(inv);
    }

    // --- Selection handling ---
    public void selectInitiative(Player player, String title) {
        selectedInitiatives.put(player.getUniqueId(), title);
    }

    public void deselectInitiative(Player player) {
        selectedInitiatives.remove(player.getUniqueId());
    }

    public String getSelected(Player player) {
        return selectedInitiatives.get(player.getUniqueId());
    }

}
