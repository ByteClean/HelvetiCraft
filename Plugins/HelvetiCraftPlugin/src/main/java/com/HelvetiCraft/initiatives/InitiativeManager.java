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

        // Prevent taking items out of initiative menus
        if (title.startsWith("§6Initiativen") || title.startsWith("§6Volksinitiativen") || title.equals("§6Deine Volksinitiativen")) {
            event.setCancelled(true);
        }

        if (clicked == null || !clicked.hasItemMeta()) return;
        // Only handle clicks for initiative menus
        if (!(title.startsWith("§6Initiativen") || title.startsWith("§6Volksinitiativen") || title.equals("§6Deine Volksinitiativen"))) return;

        Inventory top = player.getOpenInventory().getTopInventory();
        int topSize = top.getSize();
        int raw = event.getRawSlot();
        if (raw < 0 || raw >= topSize) return;

        event.setCancelled(true);
        int phase = InitiativeRequests.getCurrentPhase(player.getUniqueId());

        // Main Initiative Menu
        if (title.startsWith("§6Volksinitiativen") || title.startsWith("§6Initiativen")) {
            String displayName = clicked.getItemMeta().getDisplayName();
            switch (phase) {
                case 0: // Phase 0: Voting
                    switch (clicked.getType()) {
                        case EMERALD -> {
                            if (InitiativeRequests.canCreateInitiative(player.getUniqueId(), player.getName())) {
                                startInitiativeCreation(player);
                            } else player.sendMessage("§cDu kannst keine weitere Initiative in Phase 0 erstellen!");
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
                    break;
                case 1: // Phase 1: Admin acceptance, no voting
                    switch (clicked.getType()) {
                        case PAPER -> {
                            // Just display info
                        }
                        case ARROW -> {
                            int page = playerPages.getOrDefault(player.getUniqueId(), 0);
                            playerPages.put(player.getUniqueId(), displayName.contains("Zurück") ? page - 1 : page + 1);
                            openInitiativeMenu(player);
                        }
                    }
                    break;
                case 2: // Phase 2: Final voting for/against
                    switch (clicked.getType()) {
                        case GREEN_WOOL, RED_WOOL -> {
                            List<String> lore = clicked.getItemMeta().getLore();
                            if (lore == null || lore.isEmpty()) return;
                            // Initiative title is the last line in lore, strip §8 prefix
                            String initiativeTitle = lore.get(lore.size() - 1).replaceAll("§.", "").trim();
                            boolean voteFor = clicked.getType() == Material.GREEN_WOOL;
                            
                            player.sendMessage(voteFor ? "§aDu hast dafür gestimmt!" : "§cDu hast dagegen gestimmt!");
                            player.sendMessage("§7Menü wird aktualisiert... Starte den Menu new um es zu sehen.");
                            
                            InitiativeRequests.votePhase2(player.getUniqueId(), initiativeTitle, voteFor);
                            
                            // Clear cache and refresh menu after longer delay
                            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                                InitiativeRequests.refreshVotes(player.getUniqueId());
                                openInitiativeMenu(player);
                            }, 20L); // 1 second delay
                        }
                        case ARROW -> {
                            int page = playerPages.getOrDefault(player.getUniqueId(), 0);
                            playerPages.put(player.getUniqueId(), displayName.contains("Zurück") ? page - 1 : page + 1);
                            openInitiativeMenu(player);
                        }
                    }
                    break;
                default: // Phase 3: Pause
                    // No actions allowed
                    player.sendMessage("§cDie Initiativen befinden sich aktuell in einer Pause.");
                    break;
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
                        InitiativeRequests.deleteInitiative(selected, player.getUniqueId());
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
                Initiative in = InitiativeRequests.getInitiative(t, player.getUniqueId());
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
        for (Initiative i : InitiativeRequests.getAllInitiatives(player.getUniqueId())) {
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
                    com.HelvetiCraft.util.SafeScheduler.runSync(plugin, () -> askForDescription(player, title.trim()));
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
                    InitiativeRequests.createInitiative(new Initiative(title, desc.trim(), player.getName()), player.getUniqueId());
                    player.sendMessage("§aVolksinitiative erstellt: §b" + title);
                    player.getScheduler().run(plugin, task -> openInitiativeMenu(player), null);
                    return List.of(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }

    private String stripColorPrefix(String displayName) {
        if (displayName == null) return "";
        return displayName.replaceAll("^§.", "");
    }
}
