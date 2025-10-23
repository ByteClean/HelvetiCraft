package com.HelvetiCraft.initiatives;

import com.HelvetiCraft.Main;
import com.HelvetiCraft.requests.InitiativeRequests;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * InitiativeManager handles the main initiative GUI and the "Meine Initiativen" sub-GUI.
 */
public class InitiativeManager implements Listener {

    private final Main plugin;
    private final InitiativeMenu menu;
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<UUID, String> selectedInitiative = new HashMap<>();

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
        if (!(title.startsWith("§6Volksinitiativen") || title.equals("§6Deine Volksinitiativen"))) return;

        Inventory top = player.getOpenInventory().getTopInventory();
        int topSize = top.getSize();
        int raw = event.getRawSlot();
        if (raw < 0 || raw >= topSize) return;

        event.setCancelled(true);
        int phase = InitiativeRequests.getCurrentPhase();

        // Main Initiative Menu
        if (title.startsWith("§6Volksinitiativen")) {
            String displayName = clicked.getItemMeta().getDisplayName();
            if (phase == 1) {
                switch (clicked.getType()) {
                    case EMERALD -> {
                        if (InitiativeRequests.canCreateInitiative(player.getUniqueId(), player.getName())) {
                            startInitiativeCreation(player);
                        } else player.sendMessage("§cDu kannst keine weitere Initiative in Phase 1 erstellen!");
                    }
                    case PAPER, GREEN_WOOL -> {
                        String voteTitle = stripColorPrefix(displayName);
                        InitiativeRequests.votePhase1(player.getUniqueId(), voteTitle);
                        openInitiativeMenu(player);
                    }
                    case WRITABLE_BOOK -> openOwnInitiativesMenu(player);
                    case ARROW -> {
                        int page = playerPages.getOrDefault(player.getUniqueId(), 0);
                        playerPages.put(player.getUniqueId(), displayName.contains("Zurück") ? page - 1 : page + 1);
                        openInitiativeMenu(player);
                    }
                }
            } else { // Phase 2
                switch (clicked.getType()) {
                    case GREEN_WOOL, RED_WOOL -> {
                        // Extract initiative title from the last line of the lore
                        List<String> lore = clicked.getItemMeta().getLore();
                        if (lore == null || lore.size() < 2) return;
                        String initiativeTitle = lore.get(lore.size() - 1).replace("§8Initiative: ", "").trim();

                        boolean voteFor = clicked.getType() == Material.GREEN_WOOL;
                        InitiativeRequests.votePhase2(player.getUniqueId(), initiativeTitle, voteFor);
                        openInitiativeMenu(player);
                    }
                    case ARROW -> {
                        int page = playerPages.getOrDefault(player.getUniqueId(), 0);
                        playerPages.put(player.getUniqueId(), displayName.contains("Zurück") ? page - 1 : page + 1);
                        openInitiativeMenu(player);
                    }
                }
            }
            return;
        }

        // "Meine Initiativen" GUI
        if (title.equals("§6Deine Volksinitiativen")) {
            Material type = clicked.getType();
            String displayName = clicked.getItemMeta().getDisplayName();

            switch (type) {
                case PAPER, ENCHANTED_BOOK -> handleInitiativeSelection(player, raw, top);
                case RED_WOOL -> {
                    String selected = selectedInitiative.get(player.getUniqueId());
                    if (selected == null) player.sendMessage("§cBitte wähle zuerst eine Initiative aus!");
                    else {
                        InitiativeRequests.deleteInitiative(selected);
                        player.sendMessage("§cDeine Initiative wurde gelöscht: §b" + selected);
                        selectedInitiative.remove(player.getUniqueId());
                        openOwnInitiativesMenu(player);
                    }
                }
                case ARROW -> {
                    selectedInitiative.remove(player.getUniqueId());
                    openInitiativeMenu(player);
                }
            }
        }
    }

    private void handleInitiativeSelection(Player player, int rawSlot, Inventory topInventory) {
        ItemStack clicked = topInventory.getItem(rawSlot);
        if (clicked == null || !clicked.hasItemMeta()) return;

        String clickedTitle = stripColorPrefix(clicked.getItemMeta().getDisplayName());
        UUID uuid = player.getUniqueId();
        String currentlySelected = selectedInitiative.get(uuid);

        if (currentlySelected != null && currentlySelected.equals(clickedTitle)) {
            selectedInitiative.remove(uuid);
            refreshOwnInitiativesInventory(topInventory, player, null);
        } else {
            selectedInitiative.put(uuid, clickedTitle);
            refreshOwnInitiativesInventory(topInventory, player, clickedTitle);
        }
    }

    private void refreshOwnInitiativesInventory(Inventory inv, Player player, String selectedTitle) {
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null || !it.hasItemMeta()) continue;
            ItemMeta meta = it.getItemMeta();
            String name = meta.getDisplayName();
            if (name == null) continue;

            if (it.getType() == Material.PAPER || it.getType() == Material.ENCHANTED_BOOK) {
                String t = stripColorPrefix(name);
                Initiative in = InitiativeRequests.getInitiative(t);
                if (in == null) continue;

                ItemStack newItem = selectedTitle != null && selectedTitle.equals(t)
                        ? createEnchantedBook(in)
                        : createPaper(in);

                inv.setItem(i, newItem);
            }
        }

        // Update Red Wool button (slot 22)
        ItemStack red = inv.getItem(22);
        if (red != null && red.getType() == Material.RED_WOOL) {
            ItemMeta rm = red.getItemMeta();
            if (rm != null) rm.setDisplayName(selectedTitle != null ? "§cLösche: §b" + selectedTitle : "§cLösche: §b<Initiative auswählen>");
            inv.setItem(22, red);
        }

        player.updateInventory();
    }

    private ItemStack createPaper(Initiative in) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta pm = paper.getItemMeta();
        if (pm != null) {
            pm.setDisplayName("§b" + in.getTitle());
            pm.setLore(Arrays.asList("§7" + in.getDescription(), "§7Erstellt von: §f" + in.getAuthor()));
            paper.setItemMeta(pm);
        }
        return paper;
    }

    private ItemStack createEnchantedBook(Initiative in) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta bm = book.getItemMeta();
        if (bm != null) {
            bm.setDisplayName("§b" + in.getTitle());
            bm.setLore(Arrays.asList("§7" + in.getDescription(), "§7Erstellt von: §f" + in.getAuthor()));
            bm.addEnchant(Enchantment.LOOTING, 1, true);
            bm.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            book.setItemMeta(bm);
        }
        return book;
    }

    private void openOwnInitiativesMenu(Player player) {
        // Show all initiatives for the player regardless of phase
        selectedInitiative.remove(player.getUniqueId());
        List<Initiative> own = new ArrayList<>();
        for (Initiative i : InitiativeRequests.getAllInitiatives()) {
            if (i.getAuthor().equalsIgnoreCase(player.getName()))
                own.add(i);
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§6Deine Volksinitiativen");
        int slot = 0;
        for (Initiative i : own) inv.setItem(slot++, createPaper(i));

        // Red Wool for deletion (slot 22)
        ItemStack red = new ItemStack(Material.RED_WOOL);
        ItemMeta rm = red.getItemMeta();
        if (rm != null) {
            rm.setDisplayName("§cLösche: §b<Initiative auswählen>");
            red.setItemMeta(rm);
        }
        inv.setItem(22, red);

        // Back button (slot 26)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        if (bm != null) {
            bm.setDisplayName("§eZurück");
            back.setItemMeta(bm);
        }
        inv.setItem(26, back);

        player.openInventory(inv);
    }

    private void startInitiativeCreation(Player player) {
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("§6Titel der Volksinitiative")
                .text("Titel eingeben")
                .itemLeft(new ItemStack(Material.PAPER))
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    String title = state.getText();
                    if (title == null || title.trim().isEmpty()) return List.of(AnvilGUI.ResponseAction.replaceInputText("§cTitel darf nicht leer sein!"));
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
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    String desc = state.getText();
                    if (desc == null || desc.trim().isEmpty()) return List.of(AnvilGUI.ResponseAction.replaceInputText("§cBeschreibung darf nicht leer sein!"));
                    InitiativeRequests.createInitiative(new Initiative(title, desc.trim(), player.getName()));
                    player.sendMessage("§aVolksinitiative erstellt: §b" + title);
                    Bukkit.getScheduler().runTask(plugin, () -> openInitiativeMenu(player));
                    return List.of(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }

    private String stripColorPrefix(String displayName) {
        if (displayName == null) return "";
        return displayName.replaceAll("^§.", "");
    }
}
