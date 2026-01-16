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
        InitiativeRequests.refreshVotes(player.getUniqueId());
        int phase = InitiativeRequests.getCurrentPhase(player.getUniqueId());
        List<Initiative> initiatives = new ArrayList<>(InitiativeRequests.getAllInitiatives(player.getUniqueId()));

        switch (phase) {
            case 1:
                openPhase1(player, page, initiatives);
                break;
            case 2:
                openPhase2AdminAcceptance(player, page, initiatives);
                break;
            case 3:
                openPhase3Voting(player, page, initiatives);
                break;
            default:
                openPauseMessage(player);
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
    // --- Phase 2: Admin acceptance, no voting in Minecraft ---
    private void openPhase2AdminAcceptance(Player player, int page, List<Initiative> list) {
        int initiativesPerPage = 18;
        int totalPages = Math.max(1, (int) Math.ceil((double) list.size() / initiativesPerPage));
        page = Math.max(0, Math.min(page, totalPages - 1));
        manager.getPlayerPages().put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 27, "§6Initiativen (Phase 2: Admin-Prüfung)");

        int start = page * initiativesPerPage;
        int end = Math.min(start + initiativesPerPage, list.size());
        for (int i = start; i < end; i++) {
            Initiative initiative = list.get(i);
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b" + initiative.getTitle());
                meta.setLore(Arrays.asList(
                        "§7Autor: " + initiative.getAuthor(),
                        "§7Beschreibung: " + initiative.getDescription(),
                        "§eStatus: " + (initiative.getPhase() == 2 ? "§aAkzeptiert" : "§cAusstehend")
                ));
                item.setItemMeta(meta);
            }
            inv.addItem(item);
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

    // --- Phase 3: Voting for/against in Minecraft ---
    private void openPhase3Voting(Player player, int page, List<Initiative> list) {
        int initiativesPerPage = 7;
        int totalPages = Math.max(1, (int) Math.ceil((double) list.size() / initiativesPerPage));
        page = Math.max(0, Math.min(page, totalPages - 1));
        manager.getPlayerPages().put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 27, "§6Initiativen (Phase 3: Abstimmung)");

        int start = page * initiativesPerPage;
        int end = Math.min(start + initiativesPerPage, list.size());
        Map<String, Boolean> playerVotes = InitiativeRequests.getPlayerVotesPhase2(player.getUniqueId());

        for (int i = start; i < end; i++) {
            Initiative initiative = list.get(i);
            int colIndex = i - start + 1;
            int topSlot = colIndex;
            int midSlot = colIndex + 9;
            int botSlot = colIndex + 18;

            // --- Paper info (top row) ---
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
            inv.setItem(topSlot, paper);

            // --- Green wool (middle row) ---
            ItemStack green = new ItemStack(Material.GREEN_WOOL);
            ItemMeta greenMeta = green.getItemMeta();
            if (greenMeta != null) {
                greenMeta.setDisplayName("§aDafür stimmen");
                if (Boolean.TRUE.equals(playerVotes.get(initiative.getTitle()))) {
                    greenMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
                }
                greenMeta.setLore(Arrays.asList(
                        "§7Klicke, um für zu stimmen",
                        "§8Initiative: " + initiative.getTitle()
                ));
                green.setItemMeta(greenMeta);
            }
            inv.setItem(midSlot, green);

            // --- Red wool (bottom row) ---
            ItemStack red = new ItemStack(Material.RED_WOOL);
            ItemMeta redMeta = red.getItemMeta();
            if (redMeta != null) {
                redMeta.setDisplayName("§cDagegen stimmen");
                if (Boolean.FALSE.equals(playerVotes.get(initiative.getTitle()))) {
                    redMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
                }
                redMeta.setLore(Arrays.asList(
                        "§7Klicke, um dagegen zu stimmen",
                        "§8Initiative: " + initiative.getTitle()
                ));
                red.setItemMeta(redMeta);
            }
            inv.setItem(botSlot, red);
        }

        // Pagination arrows
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName("§eZurück");
                prev.setItemMeta(prevMeta);
            }
            inv.setItem(9, prev);
        }
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§eWeiter");
                next.setItemMeta(nextMeta);
            }
            inv.setItem(17, next);
        }

        player.openInventory(inv);
    }

    // --- Phase 4: Pause ---
    private void openPauseMessage(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, "§6Initiativen (Pause)");
        ItemStack info = new ItemStack(Material.BARRIER);
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cKeine Initiativen möglich");
            meta.setLore(Collections.singletonList("§7Die Initiativen befinden sich aktuell in einer Pause."));
            info.setItemMeta(meta);
        }
        inv.setItem(4, info);
        player.openInventory(inv);
    }
}
