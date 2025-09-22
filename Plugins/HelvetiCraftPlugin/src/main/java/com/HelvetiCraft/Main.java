package com.helveticraft.helveticraftplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.*;

public class Main extends JavaPlugin implements Listener {

    // Dummy in-memory initiatives with vote counts
    private final Map<String, Initiative> initiatives = new HashMap<>();
    // Map<PlayerUUID, Set<initiativeTitle>>
    private final Map<UUID, Set<String>> playerVotes = new HashMap<>();
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    @Override
    public void onEnable() {
        getLogger().info("HelvetiCraft Plugin has been enabled!");
        saveDefaultConfig();

        // Register listener
        getServer().getPluginManager().registerEvents(this, this);

        // Add some dummy initiatives
        initiatives.put("Build a Park", new Initiative(
                "Build a Park",
                "Create a new community park in the spawn area",
                "Alice",
                5
        ));
        initiatives.put("New Spawn Monument", new Initiative(
                "New Spawn Monument",
                "Erect a monument for the server anniversary",
                "Bob",
                3
        ));
        initiatives.put("Community Farm", new Initiative(
                "Community Farm",
                "Build a farm to share resources among players",
                "Charlie",
                7
        ));
        // Add some dummy initiatives
        initiatives.put("Build a Park", new Initiative(
                "Build a Park",
                "Create a new community park in the spawn area",
                "Alice",
                5
        ));
        initiatives.put("New Spawn Monument", new Initiative(
                "New Spawn Monument",
                "Erect a monument for the server anniversary",
                "Bob",
                3
        ));
        initiatives.put("Community Farm", new Initiative(
                "Community Farm",
                "Build a farm to share resources among players",
                "Charlie",
                7
        ));

// --- Extra dummy initiatives for pagination testing ---
        initiatives.put("Expand Nether Hub", new Initiative(
                "Expand Nether Hub",
                "Improve and expand the Nether transportation hub",
                "Diana",
                4
        ));
        initiatives.put("Server Marketplace", new Initiative(
                "Server Marketplace",
                "Add a marketplace at spawn for trading",
                "Ethan",
                6
        ));
        initiatives.put("Arena Build", new Initiative(
                "Arena Build",
                "Create a PvP and mob arena for events",
                "Fiona",
                2
        ));
        initiatives.put("Railway System", new Initiative(
                "Railway System",
                "Build a railway network between major towns",
                "George",
                8
        ));
        initiatives.put("Library Project", new Initiative(
                "Library Project",
                "Central library with enchanting and books",
                "Hannah",
                1
        ));
        initiatives.put("Community Storage", new Initiative(
                "Community Storage",
                "Shared storage system for resources",
                "Ian",
                9
        ));
        initiatives.put("Spawn Expansion", new Initiative(
                "Spawn Expansion",
                "Expand spawn with new buildings and paths",
                "Jack",
                4
        ));
        initiatives.put("Fishing Event Area", new Initiative(
                "Fishing Event Area",
                "Dedicated place for fishing competitions",
                "Karen",
                2
        ));
        initiatives.put("Town Hall", new Initiative(
                "Town Hall",
                "Build a central town hall for meetings",
                "Liam",
                5
        ));
        initiatives.put("Parkour Course", new Initiative(
                "Parkour Course",
                "Challenging parkour course with rewards",
                "Mia",
                3
        ));
        initiatives.put("Sky Island", new Initiative(
                "Sky Island",
                "Floating island project in the sky",
                "Noah",
                7
        ));
        initiatives.put("Dungeon Build", new Initiative(
                "Dungeon Build",
                "Player-made dungeon with mobs and loot",
                "Olivia",
                6
        ));
        initiatives.put("Village Expansion", new Initiative(
                "Village Expansion",
                "Expand and secure villages with walls",
                "Paul",
                4
        ));
        initiatives.put("Harbor Project", new Initiative(
                "Harbor Project",
                "Create a functional harbor with ships",
                "Quinn",
                5
        ));
        initiatives.put("Redstone Academy", new Initiative(
                "Redstone Academy",
                "A place to learn and teach redstone",
                "Ryan",
                2
        ));
        initiatives.put("Mountain Base", new Initiative(
                "Mountain Base",
                "Community base carved into the mountains",
                "Sophia",
                8
        ));
        initiatives.put("Zoo Project", new Initiative(
                "Zoo Project",
                "Build a zoo with different mob enclosures",
                "Tom",
                6
        ));
        initiatives.put("Highway System", new Initiative(
                "Highway System",
                "Nether highways for fast travel",
                "Uma",
                7
        ));
        initiatives.put("Castle Build", new Initiative(
                "Castle Build",
                "Massive castle for roleplay and events",
                "Victor",
                3
        ));

    }

    @Override
    public void onDisable() {
        getLogger().info("HelvetiCraft Plugin has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        switch (command.getName().toLowerCase()) {

            case "initiative":
                openInitiativeMenu(player);
                return true;

            case "verify":
                if (!player.hasPermission("helveticraft.verify")) {
                    player.sendMessage("§cYou do not have permission to use this command.");
                    return true;
                }
                String uniqueCode = "ABC123"; // TODO: call backend
                player.sendMessage("§aYour verification code is: §e" + uniqueCode);
                player.sendMessage("§7Please go to our Discord and run §b/verify " + uniqueCode + " §7to link your account.");
                return true;

            case "status":
                player.sendMessage("§6--- HelvetiCraft Project Status ---");
                player.sendMessage("§aStage: §e" + getConfig().getString("PROJECT_STAGE", "Unknown"));
                player.sendMessage("§aServer Online: §e" + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
                player.sendMessage("§aNext Update: §e" + getConfig().getString("NEXT_UPDATE", "TBD"));
                return true;

            case "helveticraft":
                player.sendMessage("§6--- HelvetiCraft Server Info ---");
                player.sendMessage("§aWelcome to HelvetiCraft! A fun and engaging Minecraft server.");

                TextComponent discordLink = new TextComponent("§bDiscord");
                discordLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                        getConfig().getString("DISCORD_URL", "https://discord.gg/placeholder")));

                TextComponent websiteLink = new TextComponent("§bWebsite");
                websiteLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                        getConfig().getString("WEBSITE_URL", "https://example.com")));

                player.spigot().sendMessage(discordLink);
                player.spigot().sendMessage(websiteLink);
                return true;
        }

        return false;
    }

    private void openInitiativeMenu(Player player) {
        int page = playerPages.getOrDefault(player.getUniqueId(), 0);
        int initiativesPerPage = 18;

        List<Initiative> initiativeList = new ArrayList<>(initiatives.values());
        int totalPages = (int) Math.ceil((double) initiativeList.size() / initiativesPerPage);

        // Clamp page number
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        playerPages.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 27, "§6Active Initiatives (Page " + (page + 1) + "/" + totalPages + ")");

        Set<String> votedSet = playerVotes.getOrDefault(player.getUniqueId(), new HashSet<>());

        // Fill initiatives (only for current page)
        int startIndex = page * initiativesPerPage;
        int endIndex = Math.min(startIndex + initiativesPerPage, initiativeList.size());
        for (int i = startIndex; i < endIndex; i++) {
            Initiative initiative = initiativeList.get(i);
            boolean voted = votedSet.contains(initiative.getTitle());
            ItemStack item = new ItemStack(voted ? Material.GREEN_WOOL : Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName((voted ? "§a" : "§b") + initiative.getTitle());
                List<String> lore = new ArrayList<>();
                lore.add("§7Author: " + initiative.getAuthor());
                lore.add("§7Description: " + initiative.getDescription());
                lore.add("§eVotes: " + initiative.getVotes());
                if (voted) lore.add("§aYou already voted!");
                else lore.add("§7[Click to Vote]");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.addItem(item);
        }

        // --- Control Row ---
        // Create Initiative
        ItemStack createItem = new ItemStack(Material.EMERALD);
        ItemMeta createMeta = createItem.getItemMeta();
        if (createMeta != null) {
            createMeta.setDisplayName("§aCreate New Initiative");
            createMeta.setLore(Collections.singletonList("§7Click to create a new initiative"));
            createItem.setItemMeta(createMeta);
        }
        inv.setItem(18, createItem);

        // Own Initiatives
        ItemStack ownItem = new ItemStack(Material.BOOK);
        ItemMeta ownMeta = ownItem.getItemMeta();
        if (ownMeta != null) {
            ownMeta.setDisplayName("§bYour Initiatives");
            ownMeta.setLore(Collections.singletonList("§7View and manage your initiatives"));
            ownItem.setItemMeta(ownMeta);
        }
        inv.setItem(19, ownItem);

        // Stats
        ItemStack statsItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta statsMeta = statsItem.getItemMeta();
        if (statsMeta != null) {
            statsMeta.setDisplayName("§6Initiative Stats");
            List<String> lore = new ArrayList<>();
            lore.add("§7Total Initiatives: §e" + initiatives.size());
            int totalVotes = initiatives.values().stream().mapToInt(Initiative::getVotes).sum();
            lore.add("§7Total Votes: §e" + totalVotes);
            statsMeta.setLore(lore);
            statsItem.setItemMeta(statsMeta);
        }
        inv.setItem(20, statsItem);

        // Prev Page
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName("§ePrevious Page");
                prevMeta.setLore(Collections.singletonList("§7Go to page " + page));
                prev.setItemMeta(prevMeta);
            }
            inv.setItem(24, prev);
        }

        // Next Page
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§eNext Page");
                nextMeta.setLore(Collections.singletonList("§7Go to page " + (page + 2)));
                next.setItemMeta(nextMeta);
            }
            inv.setItem(25, next);
        }

        player.openInventory(inv);
    }

    // Prevent taking items out and handle clicks
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!event.getView().getTitle().startsWith("§6Active Initiatives")) return;

        event.setCancelled(true); // prevent item movement
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String displayName = clicked.getItemMeta().getDisplayName();
        String title = displayName.replace("§b", "").replace("§a", "");

        if (clicked.getType() == Material.PAPER || clicked.getType() == Material.GREEN_WOOL) {
            Initiative initiative = initiatives.get(title);
            if (initiative == null) return;

            Set<String> votedSet = playerVotes.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());

            if (votedSet.contains(title)) {
                // Toggle off vote
                votedSet.remove(title);
                initiative.decrementVotes(); // add a decrement method in Initiative
                player.sendMessage("§cYou removed your vote for §b" + title);
            } else {
                // Vote
                votedSet.add(title);
                initiative.incrementVotes();
                player.sendMessage("§aYou voted for §b" + title + "! Current votes: §e" + initiative.getVotes());
            }

            openInitiativeMenu(player); // refresh menu
        } else if (clicked.getType() == Material.EMERALD) {
            player.closeInventory();
            startInitiativeCreation(player);
        }
        else if (clicked.getType() == Material.ARROW) {
            if (displayName.contains("Previous")) {
                playerPages.put(player.getUniqueId(), playerPages.getOrDefault(player.getUniqueId(), 0) - 1);
            } else if (displayName.contains("Next")) {
                playerPages.put(player.getUniqueId(), playerPages.getOrDefault(player.getUniqueId(), 0) + 1);
            }
            openInitiativeMenu(player);
        }
    }

    private void startInitiativeCreation(Player player) {
        new AnvilGUI.Builder()
                .plugin(this)
                .title("§6New Initiative Title")
                .text("Enter Title")
                .itemLeft(new ItemStack(Material.PAPER))
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();

                    String text = stateSnapshot.getText();
                    Player p = stateSnapshot.getPlayer();

                    if (text == null || text.trim().isEmpty()) {
                        return AnvilGUI.Response.text("§cTitle cannot be empty!");
                    }

                    // Open description GUI safely on the next server tick
                    Bukkit.getScheduler().runTask(this, () -> askForDescription(p, text.trim()));

                    return AnvilGUI.Response.close();
                })
                .open(player); // no preventClose here
    }

    private void askForDescription(Player player, String title) {
        new AnvilGUI.Builder()
                .plugin(this)
                .title("§6New Initiative Description")
                .text("Enter Description")
                .itemLeft(new ItemStack(Material.BOOK))
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();

                    String text = stateSnapshot.getText();
                    Player p = stateSnapshot.getPlayer();

                    if (text == null || text.trim().isEmpty()) {
                        // Let the player fix it without closing
                        return AnvilGUI.Response.text("§cDescription cannot be empty!");
                    }

                    // Add new initiative
                    Initiative newInit = new Initiative(title, text.trim(), p.getName(), 0);
                    initiatives.put(title, newInit);
                    p.sendMessage("§aInitiative created: §b" + title);

                    // Open the initiative menu after creation
                    openInitiativeMenu(p);

                    // Close the AnvilGUI
                    return AnvilGUI.Response.close();
                })
                .open(player); // no preventClose here
    }


    // Dummy class for initiatives with votes
    static class Initiative {
        private final String title;
        private final String description;
        private final String author;
        private int votes;

        public Initiative(String title, String description, String author, int votes) {
            this.title = title;
            this.description = description;
            this.author = author;
            this.votes = votes;
        }

        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getAuthor() { return author; }
        public int getVotes() { return votes; }
        public void incrementVotes() { votes++; }
        public void decrementVotes() {
            if (votes > 0) votes--;
        }
    }
}
