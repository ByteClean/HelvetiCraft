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
    private final InitiativeMenu initiativeMenu;
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public InitiativeManager(Main plugin) {
        this.plugin = plugin;
        this.initiativeMenu = new InitiativeMenu(this);

        long durationDays = plugin.getConfig().getLong("initiatives.phases.duration-days", 3);
        int totalRounds = plugin.getConfig().getInt("initiatives.phases.rounds", 5);
        InitiativeRequests.setPhaseConfig((int) durationDays, totalRounds);

        // Schedule phase advancement
        Bukkit.getScheduler().runTaskTimer(plugin, InitiativeRequests::advancePhase, 20L, 20L);
    }

    // --- PAPI placeholders for manager ---
    public int getCurrentPhase() { return InitiativeRequests.getCurrentPhase(); }
    public int getPastPhases() { return InitiativeRequests.getPastPhases(); }
    public long getPhaseEndTime() { return InitiativeRequests.getPhaseEndTime(); }

    public void openInitiativeMenu(Player player) {
        int page = playerPages.getOrDefault(player.getUniqueId(), 0);
        initiativeMenu.open(player, page);
    }

    public Map<UUID, Integer> getPlayerPages() { return playerPages; }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String title = event.getView().getTitle();
        if (!title.startsWith("§6Aktive Volksinitiativen") && !title.startsWith("§6Meine Volksinitiativen")) return;

        event.setCancelled(true);
        String displayName = clicked.getItemMeta().getDisplayName();

        if (title.startsWith("§6Aktive Volksinitiativen")) {
            String initiativeTitle = displayName.replace("§b", "").replace("§a", "");
            switch (clicked.getType()) {
                case PAPER:
                case GREEN_WOOL:
                    InitiativeRequests.vote(player.getUniqueId(), initiativeTitle);
                    openInitiativeMenu(player);
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
                    int page = playerPages.getOrDefault(player.getUniqueId(), 0);
                    playerPages.put(player.getUniqueId(), displayName.contains("Zurück") ? page - 1 : page + 1);
                    openInitiativeMenu(player);
                    break;
            }
        } else if (title.startsWith("§6Meine Volksinitiativen")) {
            String selected = initiativeMenu.getSelected(player);
            switch (clicked.getType()) {
                case PAPER:
                case ENCHANTED_BOOK:
                    String initiativeName = displayName.replace("§b", "");
                    if (initiativeName.equals(selected)) initiativeMenu.deselectInitiative(player);
                    else initiativeMenu.selectInitiative(player, initiativeName);
                    initiativeMenu.openPlayerInitiatives(player, playerPages.getOrDefault(player.getUniqueId(), 0));
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
                    InitiativeRequests.deleteInitiative(selected);
                    initiativeMenu.deselectInitiative(player);
                    player.sendMessage("§cVolksinitiative §b" + selected + " §cwurde gelöscht.");
                    initiativeMenu.openPlayerInitiatives(player, playerPages.getOrDefault(player.getUniqueId(), 0));
                    break;
            }
        }
    }

    // --- Creation / Editing ---
    private void startInitiativeCreation(Player player) {
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("§6Neuer Titel der Volksinitiative")
                .text("Titel eingeben")
                .itemLeft(new ItemStack(Material.PAPER))
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    String text = state.getText();
                    if (text == null || text.trim().isEmpty())
                        return AnvilGUI.Response.text("§cTitel darf nicht leer sein!");
                    Bukkit.getScheduler().runTask(plugin, () -> askForDescription(player, text.trim()));
                    return AnvilGUI.Response.close();
                }).open(player);
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
                    if (text == null || text.trim().isEmpty())
                        return AnvilGUI.Response.text("§cBeschreibung darf nicht leer sein!");
                    InitiativeRequests.createInitiative(new Initiative(title, text.trim(), player.getName(), 0));
                    player.sendMessage("§aVolksinitiative erstellt: §b" + title);
                    openInitiativeMenu(player);
                    return AnvilGUI.Response.close();
                }).open(player);
    }

    private void editInitiative(Player player, String title) {
        Initiative initiative = InitiativeRequests.getInitiative(title);
        if (initiative == null) return;

        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("§6Beschreibung bearbeiten")
                .text(initiative.getDescription())
                .itemLeft(new ItemStack(Material.BOOK))
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    String text = state.getText();
                    if (text == null || text.trim().isEmpty())
                        return AnvilGUI.Response.text("§cBeschreibung darf nicht leer sein!");
                    initiative.setDescription(text.trim());
                    InitiativeRequests.updateInitiative(initiative);
                    player.sendMessage("§aBeschreibung der Volksinitiative §b" + title + " §awurde aktualisiert!");
                    initiativeMenu.openPlayerInitiatives(player, playerPages.getOrDefault(player.getUniqueId(), 0));
                    return AnvilGUI.Response.close();
                }).open(player);
    }
}
