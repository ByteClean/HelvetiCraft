package com.HelvetiCraft.Claims;

import com.HelvetiCraft.finance.FinanceManager;
import com.HelvetiCraft.requests.TaxRequests;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Logger;

public class LandTaxManager {

    private final JavaPlugin plugin;
    private final Logger log;
    private final FinanceManager financeManager;

    public LandTaxManager(JavaPlugin plugin, FinanceManager financeManager) {
        this.plugin = plugin;
        this.financeManager = financeManager;
        this.log = plugin.getLogger();
    }

    public void collectLandTax() {
        log.info("[LandTax] Starte Berechnung der Landsteuer...");

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            try {
                GriefPrevention gp = GriefPrevention.instance;
                if (gp == null) {
                    log.warning("[LandTax] GriefPrevention-Instanz ist null – Abbruch.");
                    return;
                }

                Collection<Claim> allClaims = gp.dataStore.getClaims();
                log.info("[LandTax] Gesamtanzahl Claims: " + allClaims.size());

                Map<UUID, List<Claim>> claimsByOwner = new HashMap<>();
                for (Claim claim : allClaims) {
                    if (claim == null || claim.ownerID == null) {
                        log.warning("[LandTax] Überspringe Claim mit ungültigem Besitzer.");
                        continue;
                    }
                    claimsByOwner.computeIfAbsent(claim.ownerID, k -> new ArrayList<>()).add(claim);
                }

                for (UUID ownerId : claimsByOwner.keySet()) {
                    List<Claim> playerClaims = claimsByOwner.get(ownerId);
                    double totalTax = 0;
                    int totalBlocks = 0;

                    // Basiswert in Promille (‰) — also z. B. 0.001 = 0.1 % des Blockwerts
                    double baseRatePromille = TaxRequests.getLandSteuerBasisPerBlock();

                    for (Claim claim : playerClaims) {
                        double locationFactor = getLocationFactor(claim);
                        double developmentFactor = getDevelopmentFactor(claim);
                        int blocks = claim.getArea();

                        // Promille umrechnen (Basiswert * Blöcke / 1000)
                        double claimTax = (baseRatePromille / 1000.0) * blocks * locationFactor * developmentFactor;
                        totalTax += claimTax;
                        totalBlocks += blocks;

                        log.info(String.format(
                                "[LandTax] Claim %s | Blöcke: %d | LageFaktor: %.2f | EntwicklungsFaktor: %.2f | Steuer: %.4f CHF",
                                claim.getID().toString(), blocks, locationFactor, developmentFactor, claimTax
                        ));
                    }

                    OfflinePlayer player = Bukkit.getOfflinePlayer(ownerId);
                    String playerName = (player != null && player.getName() != null) ? player.getName() : ownerId.toString();

                    log.info(String.format(
                            "[LandTax] Gesamtsteuer für %s (%s): %.4f CHF (%d Blöcke über %d Claims)",
                            playerName, ownerId, totalTax, totalBlocks, playerClaims.size()
                    ));

                    financeManager.ensureAccount(ownerId);
                    long taxInCents = Math.round(totalTax * 100);
                    long currentBalance = financeManager.getMain(ownerId);

                    boolean success = false;
                    if (currentBalance >= taxInCents) {
                        financeManager.addToMain(ownerId, -taxInCents);
                        success = true;
                        log.info(String.format(
                                "[LandTax] Von %s wurde %.4f CHF erfolgreich abgebucht. Neuer Kontostand: %.2f CHF",
                                playerName, totalTax, (currentBalance - taxInCents) / 100.0
                        ));
                    } else {
                        log.warning(String.format(
                                "[LandTax] %s konnte nicht genug zahlen (%.4f CHF erforderlich, %.2f CHF verfügbar). Kein Abzug vorgenommen.",
                                playerName, totalTax, currentBalance / 100.0
                        ));
                    }

                    Player onlinePlayer = Bukkit.getPlayer(ownerId);
                    if (onlinePlayer != null && onlinePlayer.isOnline()) {
                        if (success) {
                            onlinePlayer.sendMessage("§a💰 Deine Landsteuer in Höhe von §e" +
                                    String.format("%.4f", totalTax) + " CHF §awurde erfolgreich abgebucht.");
                        } else {
                            onlinePlayer.sendMessage("§c⚠ Deine Landsteuer beträgt §e" +
                                    String.format("%.4f", totalTax) + " CHF§c, aber du hast nur §e" +
                                    String.format("%.2f", currentBalance / 100.0) + " CHF§c auf deinem Konto!");
                            onlinePlayer.sendMessage("§7Bitte zahle deine Landsteuer bald, um Strafen zu vermeiden.");
                        }
                    }
                }

                log.info("[LandTax] Berechnung und Abbuchung abgeschlossen.");

            } catch (Exception e) {
                log.severe("[LandTax] Fehler im Async-Task: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private double getLocationFactor(Claim claim) {
        double factor = 1.0;
        try {
            int x = claim.getLesserBoundaryCorner().getBlockX();
            int z = claim.getLesserBoundaryCorner().getBlockZ();
            if (Math.abs(x) < 500 && Math.abs(z) < 500) {
                factor = 2.0;
            }
            log.info("[LandTax] Lagefaktor für Claim " + claim.getID() + ": " + factor);
        } catch (Exception e) {
            log.warning("[LandTax] Fehler bei Lagefaktor: " + e.getMessage());
        }
        return factor;
    }

    private double getDevelopmentFactor(Claim claim) {
        double factor = 1.0;
        try {
            GriefPrevention gp = GriefPrevention.instance;
            if (gp != null) {
                int nearbyClaims = 0;
                for (Claim other : gp.dataStore.getClaims()) {
                    if (other == claim || other.ownerID == null) continue;
                    double distance = claim.getLesserBoundaryCorner().distance(other.getLesserBoundaryCorner());
                    if (distance < 100) {
                        nearbyClaims++;
                    }
                }
                factor = 1.0 + (nearbyClaims / 10.0);
                log.info("[LandTax] Entwicklungsfaktor für Claim " + claim.getID() +
                        ": " + factor + " (Nachbarn: " + nearbyClaims + ")");
            }
        } catch (Exception e) {
            log.warning("[LandTax] Fehler bei Entwicklungsfaktor: " + e.getMessage());
        }
        return factor;
    }
}
