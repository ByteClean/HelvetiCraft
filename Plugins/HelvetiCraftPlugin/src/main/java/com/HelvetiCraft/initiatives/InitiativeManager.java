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
        initiatives.put("Park bauen", new Initiative("Park bauen",
                "Errichte einen neuen Gemeinschaftspark am Spawn", "Alice", 5));
        initiatives.put("Neues Spawn-Denkmal", new Initiative("Neues Spawn-Denkmal",
                "Baue ein Denkmal für das Server-Jubiläum", "Bob", 3));
        initiatives.put("Gemeinschaftsfarm", new Initiative("Gemeinschaftsfarm",
                "Baue eine Farm, um Ressourcen mit allen zu teilen", "Charlie", 7));
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

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String title = event.getView().getTitle();

        // Only cancel in your menus
        if (title.startsWith("§6Aktive Volksinitiativen") || title.startsWith("§6Meine Volksinitiativen")) {
            event.setCancelled(true);

            String displayName = clicked.getItemMeta().getDisplayName();

            // --- Active Volksinitiativen Menu ---
            if (title.startsWith("§6Aktive Volksinitiativen")) {
                String initiativeTitle = displayName.replace("§b", "").replace("§a", "");
                switch (clicked.getType()) {
                    case PAPER:
                    case GREEN_WOOL:
                        handleVote(player, initiativeTitle);
                        break;
                    case EMERALD:
                        player.closeInventory();
                        startInitiativeCreation(player);
                        break;
                    case BOOK:
                        player.closeInventory();
                        initiativeMenu.openPlayerInitiatives(player, 0);
                        break;
                    case ARROW:
                        if (displayName.contains("Zurück")) {
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

            // --- My Volksinitiativen Menu ---
            else if (title.startsWith("§6Meine Volksinitiativen")) {
                String selected = initiativeMenu.getSelected(player);

                switch (clicked.getType()) {
                    case PAPER:
                    case ENCHANTED_BOOK:
                        String initiativeName = displayName.replace("§b", "");
                        if (initiativeName.equals(selected)) {
                            initiativeMenu.deselectInitiative(player);
                        } else {
                            initiativeMenu.selectInitiative(player, initiativeName);
                        }
                        initiativeMenu.openPlayerInitiatives(player,
                                playerPages.getOrDefault(player.getUniqueId(), 0));
                        break;

                    case ARROW:
                        player.closeInventory();
                        openInitiativeMenu(player);
                        break;

                    case YELLOW_WOOL:
                        if (selected == null) {
                            player.sendMessage("§cKeine Volksinitiative zum Bearbeiten ausgewählt!");
                            return;
                        }
                        player.closeInventory();
                        editInitiative(player, selected);
                        break;

                    case RED_WOOL:
                        if (selected == null) {
                            player.sendMessage("§cKeine Volksinitiative zum Löschen ausgewählt!");
                            return;
                        }
                        initiatives.remove(selected);
                        initiativeMenu.deselectInitiative(player);
                        player.sendMessage("§cVolksinitiative §b" + selected + " §cwurde gelöscht.");
                        initiativeMenu.openPlayerInitiatives(player,
                                playerPages.getOrDefault(player.getUniqueId(), 0));
                        break;
                }
            }
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
            player.sendMessage("§cDu hast deine Stimme für §b" + title + " §czurückgezogen.");
        } else {
            votedSet.add(title);
            initiative.incrementVotes();
            player.sendMessage("§aDu hast für §b" + title + " §aabgestimmt.");
        }
        openInitiativeMenu(player);
    }

    // --- Volksinitiative Creation ---
    private void startInitiativeCreation(Player player) {
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("§6Neuer Titel der Volksinitiative")
                .text("Titel eingeben")
                .itemLeft(new ItemStack(Material.PAPER))
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();

                    String text = state.getText();
                    if (text == null || text.trim().isEmpty()) {
                        return AnvilGUI.Response.text("§cTitel darf nicht leer sein!");
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
                .title("§6Beschreibung der Volksinitiative")
                .text("Beschreibung eingeben")
                .itemLeft(new ItemStack(Material.BOOK))
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();

                    String text = state.getText();
                    if (text == null || text.trim().isEmpty()) {
                        return AnvilGUI.Response.text("§cBeschreibung darf nicht leer sein!");
                    }

                    initiatives.put(title, new Initiative(
                            title, text.trim(), player.getName(), 0));
                    player.sendMessage("§aVolksinitiative erstellt: §b" + title);
                    openInitiativeMenu(player);
                    return AnvilGUI.Response.close();
                })
                .open(player);
    }

    private void editInitiative(Player player, String title) {
        Initiative initiative = initiatives.get(title);
        if (initiative == null) return;

        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("§6Beschreibung bearbeiten")
                .text(initiative.getDescription())
                .itemLeft(new ItemStack(Material.BOOK))
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();

                    String text = state.getText();
                    if (text == null || text.trim().isEmpty()) {
                        return AnvilGUI.Response.text("§cBeschreibung darf nicht leer sein!");
                    }

                    initiative.setDescription(text.trim());
                    player.sendMessage("§aBeschreibung der Volksinitiative §b" + title + " §awurde aktualisiert!");
                    initiativeMenu.openPlayerInitiatives(player,
                            playerPages.getOrDefault(player.getUniqueId(), 0));
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
