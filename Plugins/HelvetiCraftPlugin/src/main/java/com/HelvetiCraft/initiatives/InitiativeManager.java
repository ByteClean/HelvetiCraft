package com.HelvetiCraft.initiatives;

import com.HelvetiCraft.Main;
import com.HelvetiCraft.requests.InitiativeRequests;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class InitiativeManager implements Listener {

    private final Main plugin;
    private final InitiativeMenu menu;
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public InitiativeManager(Main plugin) {
        this.plugin = plugin;
        this.menu = new InitiativeMenu(this);
    }

    public void openInitiativeMenu(Player player) {
        menu.open(player, playerPages.getOrDefault(player.getUniqueId(), 0));
    }

    public Map<UUID, Integer> getPlayerPages() {
        return playerPages;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        if (!title.startsWith("§6Volksinitiativen")) return;

        event.setCancelled(true);
        String displayName = clicked.getItemMeta().getDisplayName();
        int phase = InitiativeRequests.getCurrentPhase();

        if (phase == 1) {
            switch (clicked.getType()) {
                case EMERALD: // Create initiative
                    if (InitiativeRequests.canCreateInitiative(player.getUniqueId(), player.getName())) {
                        startInitiativeCreation(player);
                    } else {
                        player.sendMessage("§cDu kannst keine weitere Initiative in Phase 1 erstellen!");
                    }
                    break;

                case PAPER:
                case GREEN_WOOL:
                    String initiativeTitle = displayName.replace("§b", "");
                    InitiativeRequests.votePhase1(player.getUniqueId(), initiativeTitle);
                    openInitiativeMenu(player);
                    break;

                case ARROW:
                    int page = playerPages.getOrDefault(player.getUniqueId(), 0);
                    playerPages.put(player.getUniqueId(), displayName.contains("Zurück") ? page - 1 : page + 1);
                    openInitiativeMenu(player);
                    break;
            }

        } else if (phase == 2) {
            switch (clicked.getType()) {
                case PAPER:
                case GREEN_WOOL:
                    InitiativeRequests.votePhase2(player.getUniqueId(),
                            displayName.replace("§b", ""), true);
                    openInitiativeMenu(player);
                    break;

                case RED_WOOL:
                    InitiativeRequests.votePhase2(player.getUniqueId(),
                            displayName.replace("§b", ""), false);
                    openInitiativeMenu(player);
                    break;

                case ARROW:
                    int page = playerPages.getOrDefault(player.getUniqueId(), 0);
                    playerPages.put(player.getUniqueId(), displayName.contains("Zurück") ? page - 1 : page + 1);
                    openInitiativeMenu(player);
                    break;
            }
        }
    }

    private void startInitiativeCreation(Player player) {
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("§6Titel der Volksinitiative")
                .text("Titel eingeben")
                .itemLeft(new ItemStack(Material.PAPER))
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT)
                        return Collections.emptyList();

                    String title = state.getText();
                    if (title == null || title.trim().isEmpty()) {
                        return List.of(AnvilGUI.ResponseAction.replaceInputText("§cTitel darf nicht leer sein!"));
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> askForDescription(player, title.trim()));
                    return List.of(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }

    private void askForDescription(Player player, String title) {
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("§6Beschreibung der Volksinitiative")
                .text("Beschreibung eingeben")
                .itemLeft(new ItemStack(Material.BOOK))
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT)
                        return Collections.emptyList();

                    String desc = state.getText();
                    if (desc == null || desc.trim().isEmpty()) {
                        return List.of(AnvilGUI.ResponseAction.replaceInputText("§cBeschreibung darf nicht leer sein!"));
                    }

                    InitiativeRequests.createInitiative(new Initiative(title, desc.trim(), player.getName()));
                    player.sendMessage("§aVolksinitiative erstellt: §b" + title);
                    Bukkit.getScheduler().runTask(plugin, () -> openInitiativeMenu(player));
                    return List.of(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }
}
