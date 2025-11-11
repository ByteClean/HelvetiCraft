package com.HelvetiCraft.commands;

import com.HelvetiCraft.finance.FinanceManager;
import com.HelvetiCraft.requests.TaxRequests;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ConvertCommand implements CommandExecutor {

    private final FinanceManager finance;

    public ConvertCommand(FinanceManager finance) {
        this.finance = finance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl verwenden.");
            return true;
        }
        if (!sender.hasPermission("helveticraft.convert")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }

        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR || hand.getAmount() == 0) {
            p.sendMessage("§cKein Item in der Hand.");
            return true;
        }

        Material type = hand.getType();
        long rate = 0;
        switch (type) {
            case COAL:
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
                rate = TaxRequests.COAL_ORE_CONVERSION;
                break;
            case RAW_IRON:
            case IRON_INGOT:
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
                rate = TaxRequests.IRON_ORE_CONVERSION;
                break;
            case RAW_COPPER:
            case COPPER_INGOT:
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
                rate = TaxRequests.COPPER_ORE_CONVERSION;
                break;
            case RAW_GOLD:
            case GOLD_INGOT:
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
            case NETHER_GOLD_ORE:
                rate = TaxRequests.GOLD_ORE_CONVERSION;
                break;
            case REDSTONE:
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
                rate = TaxRequests.REDSTONE_ORE_CONVERSION;
                break;
            case LAPIS_LAZULI:
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
                rate = TaxRequests.LAPIS_ORE_CONVERSION;
                break;
            case DIAMOND:
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
                rate = TaxRequests.DIAMOND_ORE_CONVERSION;
                break;
            case EMERALD:
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
                rate = TaxRequests.EMERALD_ORE_CONVERSION;
                break;
            case QUARTZ:
            case NETHER_QUARTZ_ORE:
                rate = TaxRequests.QUARTZ_ORE_CONVERSION;
                break;
            case ANCIENT_DEBRIS:
            case NETHERITE_SCRAP:
                rate = TaxRequests.ANCIENT_DEBRIS_CONVERSION;
                break;
            default:
                p.sendMessage("§cDieses Item kann nicht konvertiert werden.");
                return true;
        }

        int amount = hand.getAmount();
        long total = rate * amount;

        hand.setAmount(0); // Remove the stack

        finance.addToMain(p.getUniqueId(), total);
        p.sendMessage("§aKonvertiert: §f" + amount + "x " + type.name() + " §afür §f" + FinanceManager.formatCents(total));

        return true;
    }
}