package com.HelvetiCraft.initiatives;

import com.helveticraft.helveticraftplugin.Main;
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
    private final InitiativeMenu initiativeMenu;

    private final Map<String, Initiative> initiatives = new HashMap<>();
    private final Map<UUID, Set<String>> playerVotes = new HashMap<>();
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public InitiativeManager(Main plugin) {
        this.plugin = plugin;
        this.initiativeMenu = new InitiativeMenu(this);

        // TODO: Replace with database later
        initiatives.put("Build a Park", new Initiative("Build a Park",
                "Create a new community park in the spawn area", "Alice", 5));
        initiatives.put("New Spawn Monument", new Initiative("New Spawn Monument",
                "Erect a monument for the server anniversary", "Bob", 3));
        initiatives.put("Community Farm", new Initiative("Community Farm",
                "Build a farm to share resources among players", "Charlie", 7));
    }

    // --- Public API ---
    public void openInitiativeMenu(Player player) {
        int page = playerPages.getOrDefault(player.getUniqueId(), 0);
        initiativeMenu.open(player, page);
    }

    public Map<String, Initiative> getInitiatives() {
        return initiatives;
    }

    public Map<UUID, Set<String>> getPlayerVotes() {
        return playerVotes;
    }

    public Map<UUID, Integer> getPlayerPages() {
        return playerPages;
    }

    // --- Events ---
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!event.getView().getTitle().startsWith("§6Active Initiatives")) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String displayName = clicked.getItemMeta().getDisplayName();
        String title = displayName.replace("§b", "").replace("§a", "");

        switch (clicked.getType()) {
            case PAPER:
            case GREEN_WOOL:
                handleVote(player, title);
                break;
            case EMERALD:
                player.closeInventory();
                startInitiativeCreation(player);
                break;
            case ARROW:
                if (displayName.contains("Previous")) {
                    playerPages.put(player.getUniqueId(),
                            playerPages.get(player.getUniqueId()) - 1);
                } else {
                    playerPages.put(player.getUniqueId(),
                            playerPages.get(player.getUniqueId()) + 1);
                }
                openInitiativeMenu(player);
                break;
        }
    }

    // --- Voting ---
    private void handleVote(Player player, String title) {
        Initiative initiative = initiatives.get(title);
        if (initiative == null) return;

        Set<String> votedSet = playerVotes
                .computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());

        if (votedSet.contains(title)) {
            votedSet.remove(title);
            initiative.decrementVotes();
            player.sendMessage("§cYou removed your vote for §b" + title);
        } else {
            votedSet.add(title);
            initiative.incrementVotes();
            player.sendMessage("§aYou voted for §b" + title);
        }
        openInitiativeMenu(player);
    }

    // --- Initiative Creation ---
    private void startInitiativeCreation(Player player) {
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("§6New Initiative Title")
                .text("Enter Title")
                .itemLeft(new ItemStack(Material.PAPER))
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();

                    String text = state.getText();
                    if (text == null || text.trim().isEmpty()) {
                        return AnvilGUI.Response.text("§cTitle cannot be empty!");
                    }

                    Bukkit.getScheduler().runTask(plugin,
                            () -> askForDescription(player, text.trim()));
                    return AnvilGUI.Response.close();
                })
                .open(player);
    }

    private void askForDescription(Player player, String title) {
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("§6New Initiative Description")
                .text("Enter Description")
                .itemLeft(new ItemStack(Material.BOOK))
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();

                    String text = state.getText();
                    if (text == null || text.trim().isEmpty()) {
                        return AnvilGUI.Response.text("§cDescription cannot be empty!");
                    }

                    initiatives.put(title, new Initiative(
                            title, text.trim(), player.getName(), 0));
                    player.sendMessage("§aInitiative created: §b" + title);
                    openInitiativeMenu(player);
                    return AnvilGUI.Response.close();
                })
                .open(player);
    }

    public int getTotalInitiatives() {
        return initiatives.size();
    }

    public int getTotalVotes() {
        return initiatives.values().stream().mapToInt(Initiative::getVotes).sum();
    }
}
