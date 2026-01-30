package com.HelvetiCraft.taxes;

import com.HelvetiCraft.Claims.ClaimManager;
import com.HelvetiCraft.finance.FinanceManager;
import com.HelvetiCraft.requests.TaxRequests;
import com.HelvetiCraft.shop.ShopTaxManager;
import com.HelvetiCraft.util.FinanceTransactionLogger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.logging.Logger;

public class VermoegensSteuerManager {

    private final JavaPlugin plugin;
    private final Logger log;
    private final FinanceManager financeManager;

    public VermoegensSteuerManager(JavaPlugin plugin, FinanceManager financeManager) {
        this.plugin = plugin;
        this.financeManager = financeManager;
        this.log = plugin.getLogger();
    }

    public void collectVermoegensSteuer() {
        log.info("[VermoegensSteuer] Starte Berechnung der Vermögenssteuer...");

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            try {
                long activeThreshold = System.currentTimeMillis() - 10L * 24 * 3600 * 1000;
                int processedPlayers = 0;
                long totalTaxCollected = 0;

                for (UUID id : financeManager.getKnownPlayers()) {
                    if (id.equals(ClaimManager.GOVERNMENT_UUID)) continue;

                    // Ensure player has a finance account (safety check)
                    financeManager.ensureAccount(id);

                    OfflinePlayer p = Bukkit.getOfflinePlayer(id);
                    String playerName = (p != null && p.getName() != null) ? p.getName() : id.toString();

                    // Skip inactive players
                    if (p.getLastLogin() < activeThreshold) {
                        log.info("[VermoegensSteuer] Überspringe inaktiven Spieler: " + playerName);
                        continue;
                    }

                    long wealth = financeManager.getMain(id) + financeManager.getSavings(id);
                    long tax = TaxRequests.calculateVermoegensSteuer(wealth);

                    if (tax <= 0) {
                        log.info(String.format(
                                "[VermoegensSteuer] %s: Kein Vermögen oder unter Freibetrag (Vermögen: %.2f CHF)",
                                playerName, wealth / 100.0
                        ));
                        continue;
                    }

                    long currentBalance = financeManager.getMain(id);
                    boolean success = false;

                    if (currentBalance >= tax) {
                        FinanceTransactionLogger logger = new FinanceTransactionLogger(financeManager);
                        logger.logTransaction("Shop-Tax", id, ClaimManager.GOVERNMENT_UUID, tax);

                        //financeManager.addToMain(id, -tax);
                        //financeManager.addToMain(ClaimManager.GOVERNMENT_UUID, tax);
                        success = true;
                        processedPlayers++;
                        totalTaxCollected += tax;

                        log.info(String.format(
                                "[VermoegensSteuer] Von %s wurde %.2f CHF erfolgreich abgebucht (Vermögen: %.2f CHF). Neuer Kontostand: %.2f CHF",
                                playerName, tax / 100.0, wealth / 100.0, (currentBalance - tax) / 100.0
                        ));
                    } else {
                        log.warning(String.format(
                                "[VermoegensSteuer] %s konnte nicht genug zahlen (%.2f CHF erforderlich, %.2f CHF verfügbar). Kein Abzug vorgenommen.",
                                playerName, tax / 100.0, currentBalance / 100.0
                        ));
                    }

                    // Notify online player
                    Player online = p.getPlayer();
                    if (online != null && online.isOnline()) {
                        if (success) {
                            online.sendMessage("§a💰 Deine Vermögenssteuer in Höhe von §e" +
                                    FinanceManager.formatCents(tax) + " §awurde erfolgreich abgebucht.");
                        } else {
                            online.sendMessage("§c⚠ Deine Vermögenssteuer beträgt §e" +
                                    FinanceManager.formatCents(tax) + "§c, aber du hast nur §e" +
                                    FinanceManager.formatCents(currentBalance) + "§c auf deinem Konto!");
                            online.sendMessage("§7Bitte zahle deine Vermögenssteuer bald, um Strafen zu vermeiden.");
                        }
                    }
                }

                log.info(String.format(
                        "[VermoegensSteuer] Berechnung abgeschlossen. %d Spieler besteuert, %.2f CHF insgesamt eingenommen.",
                        processedPlayers, totalTaxCollected / 100.0
                ));

            } catch (Exception e) {
                log.severe("[VermoegensSteuer] Fehler im Async-Task: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
