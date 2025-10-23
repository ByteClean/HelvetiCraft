package com.HelvetiCraft.initiatives;

import com.HelvetiCraft.requests.InitiativeRequests;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class InitiativeMenu {

    private final InitiativeManager manager;

    // Track selected own initiative for deletion
    private final Map<UUID, String> selectedOwnInitiative = new HashMap<>();

    public InitiativeMenu(InitiativeManager manager) {
        this.manager = manager;
    }

    public void open(Player player, int page) {
        int initiativesPerPage = 18;
        int phase = InitiativeRequests.getCurrentPhase();
        List<Initiative> list = new ArrayList<>(InitiativeRequests.getAllInitiatives());
        list.removeIf(i -> i.getPhase() != phase);

        int totalPages = Math.max(1, (int) Math.ceil((double) list.size() / initiativesPerPage));
        page = Math.max(0, Math.min(page, totalPages - 1));
        manager.getPlayerPages().put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 27, "§6Volksinitiativen (Phase " + phase + ")");

        int start = page * initiativesPerPage;
        int end = Math.min(start + initiativesPerPage, list.size());

        for (int i = start; i < end; i++) {
            Initiative initiative = list.get(i);
            ItemStack item;

            if (phase == 1) {
                boolean hasVoted = InitiativeRequests.getPlayerVotesPhase1(player.getUniqueId()).contains(initiative.getTitle());
                item = new ItemStack(hasVoted ? Material.GREEN_WOOL : Material.PAPER);
            } else {
                Map<String, Boolean> votes = InitiativeRequests.getPlayerVotesPhase2(player.getUniqueId());
                Boolean vote = votes.get(initiative.getTitle());
                if (vote == null) item = new ItemStack(Material.PAPER);
                else item = new ItemStack(vote ? Material.GREEN_WOOL : Material.RED_WOOL);
            }

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b" + initiative.getTitle());
                List<String> lore = new ArrayList<>();
                lore.add("§7Autor: " + initiative.getAuthor());
                lore.add("§7Beschreibung: " + initiative.getDescription());

                if (phase == 1) {
                    boolean hasVoted = InitiativeRequests.getPlayerVotesPhase1(player.getUniqueId()).contains(initiative.getTitle());
                    lore.add("§eStimmen: " + initiative.getVotes());
                    lore.add(hasVoted ? "§aDu hast bereits abgestimmt!" : "§7[Klicken zum Abstimmen]");
                } else {
                    Map<String, Boolean> votes = InitiativeRequests.getPlayerVotesPhase2(player.getUniqueId());
                    Boolean vote = votes.get(initiative.getTitle());
                    lore.add("§eFür: " + initiative.getVotesFor() + " / Gegen: " + initiative.getVotesAgainst());
                    lore.add(vote == null ? "§7[Klicken für / gegen]" : (vote ? "§aDu stimmst dafür" : "§cDu stimmst dagegen"));
                }

                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            inv.addItem(item);
        }

        // Phase 1 creation button
        if (phase == 1 && InitiativeRequests.canCreateInitiative(player.getUniqueId(), player.getName())) {
            ItemStack create = new ItemStack(Material.EMERALD);
            ItemMeta meta = create.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§aNeue Volksinitiative erstellen");
                meta.setLore(Collections.singletonList("§7Klicke, um eine neue Volksinitiative zu starten"));
                create.setItemMeta(meta);
            }
            inv.setItem(18, create);
        }

        // Own initiatives button (Book & Quill)
        if (phase == 1) {
            ItemStack own = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta meta = own.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6Meine Initiativen");
                meta.setLore(Collections.singletonList("§7Klicke, um deine eigenen Initiativen zu verwalten"));
                own.setItemMeta(meta);
            }
            inv.setItem(19, own);
        }

        // Pagination
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§eZurück");
                prev.setItemMeta(meta);
            }
            inv.setItem(24, prev);
        }
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§eWeiter");
                next.setItemMeta(meta);
            }
            inv.setItem(25, next);
        }

        player.openInventory(inv);
    }

    public void openOwnInitiativesMenu(Player player) {
        int phase = InitiativeRequests.getCurrentPhase();
        if (phase != 1) return; // only in phase 1

        List<Initiative> own = new ArrayList<>();
        for (Initiative i : InitiativeRequests.getAllInitiatives()) {
            if (i.getAuthor().equals(player.getName()) && i.getPhase() == 1)
                own.add(i);
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§6Meine Initiativen");
        int index = 0;
        for (Initiative i : own) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b" + i.getTitle());
                meta.setLore(Collections.singletonList("§7[Klicken zum Auswählen]"));
                item.setItemMeta(meta);
            }
            inv.setItem(index++, item);
        }

        // Red Wool for deletion
        ItemStack delete = new ItemStack(Material.RED_WOOL);
        ItemMeta metaDel = delete.getItemMeta();
        if (metaDel != null) {
            metaDel.setDisplayName("§cAusgewählte Initiative löschen");
            metaDel.setLore(Collections.singletonList("§7Klicke, um die ausgewählte Initiative zu löschen"));
            delete.setItemMeta(metaDel);
        }
        inv.setItem(26, delete);

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta metaBack = back.getItemMeta();
        if (metaBack != null) {
            metaBack.setDisplayName("§eZurück");
            back.setItemMeta(metaBack);
        }
        inv.setItem(25, back);

        player.openInventory(inv);
    }

    public void selectOwnInitiative(Player player, String title) {
        selectedOwnInitiative.put(player.getUniqueId(), title);
        player.sendMessage("§aInitiative ausgewählt: §b" + title);
    }

    public void deleteSelectedInitiative(Player player) {
        UUID uuid = player.getUniqueId();
        String title = selectedOwnInitiative.get(uuid);
        if (title == null) {
            player.sendMessage("§cKeine Initiative ausgewählt!");
            return;
        }
        InitiativeRequests.deleteInitiative(title);
        selectedOwnInitiative.remove(uuid);
        player.sendMessage("§cInitiative gelöscht: §b" + title);
        openOwnInitiativesMenu(player);
    }

    public void backToMainMenu(Player player) {
        selectedOwnInitiative.remove(player.getUniqueId());
        int page = manager.getPlayerPages().getOrDefault(player.getUniqueId(), 0);
        open(player, page);
    }

    public Map<UUID, String> getSelectedOwnInitiative() {
        return selectedOwnInitiative;
    }
}
