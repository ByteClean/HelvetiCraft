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
                boolean voted = InitiativeRequests.getPlayerVotesPhase1(player.getUniqueId()).contains(initiative.getTitle());
                item = new ItemStack(voted ? Material.GREEN_WOOL : Material.PAPER);
            } else {
                Map<String, Boolean> votes = InitiativeRequests.getPlayerVotesPhase2(player.getUniqueId());
                Boolean vote = votes.get(initiative.getTitle());
                item = new ItemStack(vote == null ? Material.PAPER : (vote ? Material.GREEN_WOOL : Material.RED_WOOL));
            }

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b" + initiative.getTitle());
                List<String> lore = new ArrayList<>();
                lore.add("§7Autor: " + initiative.getAuthor());
                lore.add("§7Beschreibung: " + initiative.getDescription());

                if (phase == 1) {
                    lore.add("§eStimmen: " + initiative.getVotes());
                    lore.add(InitiativeRequests.getPlayerVotesPhase1(player.getUniqueId()).contains(initiative.getTitle()) ?
                            "§aDu hast bereits abgestimmt!" : "§7[Klicken zum Abstimmen]");
                } else {
                    lore.add("§eFür: " + initiative.getVotesFor() + " / Gegen: " + initiative.getVotesAgainst());
                    Map<String, Boolean> votes = InitiativeRequests.getPlayerVotesPhase2(player.getUniqueId());
                    Boolean vote = votes.get(initiative.getTitle());
                    lore.add(vote == null ? "§7[Klicken für / gegen]" :
                            (vote ? "§aDu stimmst dafür" : "§cDu stimmst dagegen"));
                }

                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.addItem(item);
        }

        // Add creation button (only phase 1)
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

        // Pagination
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            if (meta != null) meta.setDisplayName("§eZurück");
            prev.setItemMeta(meta);
            inv.setItem(24, prev);
        }

        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            if (meta != null) meta.setDisplayName("§eWeiter");
            next.setItemMeta(meta);
            inv.setItem(25, next);
        }

        player.openInventory(inv);
    }
}
