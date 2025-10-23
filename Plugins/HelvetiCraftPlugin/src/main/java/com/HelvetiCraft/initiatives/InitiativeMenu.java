package com.HelvetiCraft.initiatives;

import com.HelvetiCraft.initiatives.Initiative;
import com.HelvetiCraft.initiatives.InitiativeManager;
import com.HelvetiCraft.requests.InitiativeRequests;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
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
        int phase = InitiativeRequests.getCurrentPhase();
        List<Initiative> initiatives = new ArrayList<>(InitiativeRequests.getAllInitiatives());

        if (phase == 1) {
            openPhase1(player, page, initiatives);
        } else if (phase == 2) {
            openPhase2(player, page, initiatives);
        }
    }

    // ------------------- Phase 1 -------------------
    private void openPhase1(Player player, int page, List<Initiative> list) {
        int initiativesPerPage = 18;
        int totalPages = Math.max(1, (int) Math.ceil((double) list.size() / initiativesPerPage));
        page = Math.max(0, Math.min(page, totalPages - 1));
        manager.getPlayerPages().put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 27, "§6Volksinitiativen (Phase 1)");

        int start = page * initiativesPerPage;
        int end = Math.min(start + initiativesPerPage, list.size());

        for (int i = start; i < end; i++) {
            Initiative initiative = list.get(i);
            boolean voted = InitiativeRequests.getPlayerVotesPhase1(player.getUniqueId()).contains(initiative.getTitle());
            ItemStack item = new ItemStack(voted ? Material.GREEN_WOOL : Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b" + initiative.getTitle());
                meta.setLore(Arrays.asList(
                        "§7Autor: " + initiative.getAuthor(),
                        "§7Beschreibung: " + initiative.getDescription(),
                        "§eStimmen: " + initiative.getVotes(),
                        voted ? "§aDu hast bereits abgestimmt!" : "§7[Klicken zum Abstimmen]"
                ));
                item.setItemMeta(meta);
            }
            inv.addItem(item);
        }

        // Create new initiative button
        if (InitiativeRequests.canCreateInitiative(player.getUniqueId(), player.getName())) {
            ItemStack create = new ItemStack(Material.EMERALD);
            ItemMeta meta = create.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§aNeue Volksinitiative erstellen");
                meta.setLore(Collections.singletonList("§7Klicke, um eine neue Volksinitiative zu starten"));
                create.setItemMeta(meta);
            }
            inv.setItem(18, create);
        }

        // Own initiatives button
        ItemStack own = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta ownMeta = own.getItemMeta();
        if (ownMeta != null) {
            ownMeta.setDisplayName("§6Meine Initiativen");
            ownMeta.setLore(Collections.singletonList("§7Klicke, um deine eigenen Initiativen zu verwalten"));
            own.setItemMeta(ownMeta);
        }
        inv.setItem(19, own);

        // Pagination
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            if (meta != null) meta.setDisplayName("§eZurück");
            inv.setItem(24, prev);
        }
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            if (meta != null) meta.setDisplayName("§eWeiter");
            inv.setItem(25, next);
        }

        player.openInventory(inv);
    }

    // ------------------- Phase 2 -------------------
    private void openPhase2(Player player, int page, List<Initiative> list) {
        int initiativesPerPage = 9;
        int totalPages = Math.max(1, (int) Math.ceil((double) list.size() / initiativesPerPage));
        page = Math.max(0, Math.min(page, totalPages - 1));
        manager.getPlayerPages().put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 27, "§6Volksinitiativen (Phase 2)");

        int start = page * initiativesPerPage;
        int end = Math.min(start + initiativesPerPage, list.size());
        Map<String, Boolean> playerVotes = InitiativeRequests.getPlayerVotesPhase2(player.getUniqueId());

        for (int i = start; i < end; i++) {
            Initiative initiative = list.get(i);
            int slot = i - start; // column position (0–8)

            // Top row: Initiative info
            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta paperMeta = paper.getItemMeta();
            if (paperMeta != null) {
                paperMeta.setDisplayName("§b" + initiative.getTitle());
                paperMeta.setLore(Arrays.asList(
                        "§7Autor: " + initiative.getAuthor(),
                        "§7Beschreibung: " + initiative.getDescription(),
                        "§eFür: " + initiative.getVotesFor() + " / Gegen: " + initiative.getVotesAgainst()
                ));
                paper.setItemMeta(paperMeta);
            }
            inv.setItem(slot, paper);

            // Middle row: vote "for"
            ItemStack green = new ItemStack(Material.GREEN_WOOL);
            ItemMeta greenMeta = green.getItemMeta();
            if (greenMeta != null) {
                greenMeta.setDisplayName("§aDafür stimmen");
                if (Boolean.TRUE.equals(playerVotes.get(initiative.getTitle()))) {
                    greenMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
                }
                greenMeta.setLore(Arrays.asList(
                        "§7Klicke, um für zu stimmen",
                        "§8Initiative: " + initiative.getTitle() // <-- NEW
                ));
                green.setItemMeta(greenMeta);
            }
            inv.setItem(slot + 9, green);

            // Bottom row: vote "against"
            ItemStack red = new ItemStack(Material.RED_WOOL);
            ItemMeta redMeta = red.getItemMeta();
            if (redMeta != null) {
                redMeta.setDisplayName("§cDagegen stimmen");
                if (Boolean.FALSE.equals(playerVotes.get(initiative.getTitle()))) {
                    redMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
                }
                redMeta.setLore(Arrays.asList(
                        "§7Klicke, um dagegen zu stimmen",
                        "§8Initiative: " + initiative.getTitle() // <-- NEW
                ));
                red.setItemMeta(redMeta);
            }
            inv.setItem(slot + 18, red);
        }

        // Pagination
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            if (meta != null) meta.setDisplayName("§eZurück");
            inv.setItem(24, prev);
        }
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            if (meta != null) meta.setDisplayName("§eWeiter");
            inv.setItem(25, next);
        }

        player.openInventory(inv);
    }
}
