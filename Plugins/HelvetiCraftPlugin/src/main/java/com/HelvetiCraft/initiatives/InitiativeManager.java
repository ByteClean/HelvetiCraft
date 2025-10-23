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
 * Selection behavior:
 * - Clicking a paper in "Meine Initiativen" toggles selection for that player.
 * - Selected initiative is shown as an ENCHANTED_BOOK.
 * - Pressing the RED_WOOL deletes the selected initiative (only in phase 1).
 * - Selection is ephemeral and not persisted after GUI close.
 */
public class InitiativeManager implements Listener {

    private final Main plugin;
    private final InitiativeMenu menu;
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    // currently selected initiative per-player when in "Meine Initiativen" GUI
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

        // ignore clicks without item or meta
        if (clicked == null || !clicked.hasItemMeta()) return;

        // Only handle our initiative GUIs
        if (!(title.startsWith("§6Volksinitiativen") || title.equals("§6Deine Volksinitiativen"))) return;

        // Get top inventory and size for raw slot checks
        Inventory top = player.getOpenInventory().getTopInventory();
        int topSize = top.getSize();
        int raw = event.getRawSlot();

        // We only want clicks inside the chest (top inventory)
        if (raw < 0 || raw >= topSize) {
            // clicking player inventory - ignore
            return;
        }

        event.setCancelled(true); // prevent moving items

        int phase = InitiativeRequests.getCurrentPhase();

        // ---------- Main Initiative Menu ----------
        if (title.startsWith("§6Volksinitiativen")) {
            String displayName = clicked.getItemMeta().getDisplayName();

            if (phase == 1) {
                switch (clicked.getType()) {
                    case EMERALD:
                        if (InitiativeRequests.canCreateInitiative(player.getUniqueId(), player.getName())) {
                            startInitiativeCreation(player);
                        } else {
                            player.sendMessage("§cDu kannst keine weitere Initiative in Phase 1 erstellen!");
                        }
                        break;

                    case PAPER:
                    case GREEN_WOOL:
                        // Title color prefix removal
                        String voteTitle = stripColorPrefix(displayName);
                        InitiativeRequests.votePhase1(player.getUniqueId(), voteTitle);
                        openInitiativeMenu(player);
                        break;

                    case WRITABLE_BOOK:
                        openOwnInitiativesMenu(player);
                        break;

                    case ARROW:
                        int page = playerPages.getOrDefault(player.getUniqueId(), 0);
                        playerPages.put(player.getUniqueId(), displayName.contains("Zurück") ? page - 1 : page + 1);
                        openInitiativeMenu(player);
                        break;
                    default:
                        break;
                }
            } else if (phase == 2) {
                switch (clicked.getType()) {
                    case PAPER:
                    case GREEN_WOOL:
                        String forTitle = stripColorPrefix(displayName);
                        InitiativeRequests.votePhase2(player.getUniqueId(), forTitle, true);
                        openInitiativeMenu(player);
                        break;
                    case RED_WOOL:
                        String againstTitle = stripColorPrefix(displayName);
                        InitiativeRequests.votePhase2(player.getUniqueId(), againstTitle, false);
                        openInitiativeMenu(player);
                        break;
                    case ARROW:
                        int page = playerPages.getOrDefault(player.getUniqueId(), 0);
                        playerPages.put(player.getUniqueId(), displayName.contains("Zurück") ? page - 1 : page + 1);
                        openInitiativeMenu(player);
                        break;
                    default:
                        break;
                }
            }
            return;
        }

        // ---------- "Meine Initiativen" GUI ----------
        if (title.equals("§6Deine Volksinitiativen")) {
            String displayName = clicked.getItemMeta().getDisplayName();
            Material type = clicked.getType();

            switch (type) {
                case PAPER:
                case ENCHANTED_BOOK:
                    // Toggle selection
                    handleInitiativeSelection(player, raw, top);
                    break;

                case RED_WOOL:
                    // Delete selected initiative (only if player selected one)
                    String selected = selectedInitiative.get(player.getUniqueId());
                    if (selected == null) {
                        player.sendMessage("§cBitte wähle zuerst eine Initiative aus!");
                    } else {
                        InitiativeRequests.deleteInitiative(selected);
                        player.sendMessage("§cDeine Initiative wurde gelöscht: §b" + selected);
                        // clear selection and refresh menu
                        selectedInitiative.remove(player.getUniqueId());
                    }
                    openOwnInitiativesMenu(player);
                    break;

                case ARROW:
                    // Back to main menu
                    selectedInitiative.remove(player.getUniqueId());
                    openInitiativeMenu(player);
                    break;

                default:
                    // ignore other slots
                    break;
            }
        }
    }

    // Toggle selection when clicking a paper/book in "Meine Initiativen".
    // rawSlot is provided to know which clicked slot inside the top inventory.
    private void handleInitiativeSelection(Player player, int rawSlot, Inventory topInventory) {
        ItemStack clicked = topInventory.getItem(rawSlot);
        if (clicked == null || !clicked.hasItemMeta()) return;

        String clickedTitle = stripColorPrefix(clicked.getItemMeta().getDisplayName());
        UUID uuid = player.getUniqueId();
        String currentlySelected = selectedInitiative.get(uuid);

        // If same clicked, unselect
        if (currentlySelected != null && currentlySelected.equals(clickedTitle)) {
            selectedInitiative.remove(uuid);
            refreshOwnInitiativesInventory(topInventory, player, null);
            return;
        }

        // Otherwise select the clicked initiative (only one at a time)
        selectedInitiative.put(uuid, clickedTitle);
        refreshOwnInitiativesInventory(topInventory, player, clickedTitle);
    }

    // Replace the slot items: selected becomes ENCHANTED_BOOK, others PAPERS. Update delete button text.
    private void refreshOwnInitiativesInventory(Inventory inv, Player player, String selectedTitle) {
        // Build mapping title->slot for items in top inventory (we only modify paper/book slots)
        Map<String, Integer> titleToSlot = new HashMap<>();
        ItemStack[] contents = inv.getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null || !it.hasItemMeta()) continue;
            ItemMeta meta = it.getItemMeta();
            String name = meta.getDisplayName();
            if (name == null) continue;
            // only consider paper/book entries (skip control buttons by position later)
            if (it.getType() == Material.PAPER || it.getType() == Material.ENCHANTED_BOOK) {
                String t = stripColorPrefix(name);
                titleToSlot.put(t, i);
            }
        }

        // Overwrite paper/book slots so that only the selected one is an enchanted book
        for (Map.Entry<String, Integer> e : titleToSlot.entrySet()) {
            String t = e.getKey();
            int slot = e.getValue();

            if (selectedTitle != null && selectedTitle.equals(t)) {
                // make enchanted book
                ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
                ItemMeta bm = book.getItemMeta();
                bm.setDisplayName("§b" + t);
                // add lore (optional): show description in lore if we can find it from InitiativeRequests
                Initiative in = InitiativeRequests.getInitiative(t);
                if (in != null) bm.setLore(Arrays.asList("§7" + in.getDescription(), "§7Erstellt von: §f" + in.getAuthor()));
                // add a harmless enchant to make it shimmer (no enchantment conflicts on enchanted book)
                bm.addEnchant(Enchantment.LOOTING, 1, true);
                bm.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                book.setItemMeta(bm);
                inv.setItem(slot, book);
            } else {
                // normal paper
                Initiative in = InitiativeRequests.getInitiative(t);
                ItemStack paper = new ItemStack(Material.PAPER);
                ItemMeta pm = paper.getItemMeta();
                pm.setDisplayName("§b" + t);
                if (in != null) pm.setLore(Arrays.asList("§7" + in.getDescription(), "§7Erstellt von: §f" + in.getAuthor()));
                paper.setItemMeta(pm);
                inv.setItem(slot, paper);
            }
        }

        // Update red wool button (slot 22) if present
        ItemStack red = inv.getItem(22);
        if (red != null && red.getType() == Material.RED_WOOL) {
            ItemMeta rm = red.getItemMeta();
            if (selectedTitle != null) {
                rm.setDisplayName("§cLösche: §b" + selectedTitle);
            } else {
                rm.setDisplayName("§cLösche: §b<Initiative auswählen>");
            }
            red.setItemMeta(rm);
            inv.setItem(22, red);
        }

        player.updateInventory();
    }

    // open the "Meine Initiativen" chest GUI with player's own initiatives
    private void openOwnInitiativesMenu(Player player) {
        if (InitiativeRequests.getCurrentPhase() != 1) {
            player.sendMessage("§cDu kannst deine Initiativen nur in Phase 1 verwalten!");
            return;
        }

        // clear any previous selection for this player (ephemeral)
        selectedInitiative.remove(player.getUniqueId());

        // collect player's initiatives (phase 1)
        List<Initiative> all = new ArrayList<>(InitiativeRequests.getAllInitiatives());
        List<Initiative> mine = new ArrayList<>();
        for (Initiative i : all) {
            if (i.getAuthor().equalsIgnoreCase(player.getName()) && i.getPhase() == 1) mine.add(i);
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§6Deine Volksinitiativen");

        int slot = 0;
        for (Initiative i : mine) {
            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta meta = paper.getItemMeta();
            meta.setDisplayName("§b" + i.getTitle());
            meta.setLore(Arrays.asList("§7" + i.getDescription(), "§7Erstellt von: §f" + i.getAuthor()));
            paper.setItemMeta(meta);
            inv.setItem(slot++, paper);
        }

        // Red Wool = Delete (slot 22)
        ItemStack red = new ItemStack(Material.RED_WOOL);
        ItemMeta redMeta = red.getItemMeta();
        redMeta.setDisplayName("§cLösche: §b<Initiative auswählen>");
        red.setItemMeta(redMeta);
        inv.setItem(22, red);

        // Back button (slot 26)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§eZurück");
        back.setItemMeta(backMeta);
        inv.setItem(26, back);

        player.openInventory(inv);
    }

    // Start creation flow (Anvil GUI title -> description)
    private void startInitiativeCreation(Player player) {
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("§6Titel der Volksinitiative")
                .text("Titel eingeben")
                .itemLeft(new ItemStack(Material.PAPER))
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
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
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
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

    // helper: remove our color prefix (we only prefix with §b in item creation)
    private String stripColorPrefix(String displayName) {
        if (displayName == null) return "";
        if (displayName.startsWith("§b")) return displayName.substring(2);
        if (displayName.startsWith("§a") || displayName.startsWith("§c") || displayName.startsWith("§e") || displayName.startsWith("§6"))
            return displayName.substring(2);
        return displayName;
    }
}
