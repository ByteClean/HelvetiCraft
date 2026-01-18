package com.HelvetiCraft.requests;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Logger;

/**
 * Tax configuration handler that loads values from the backend database.
 * All values are dynamically fetched from the backend and can be updated via API.
 */
public class TaxRequests {

    private static String API_BASE;
    private static String API_KEY;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();
    private static Logger logger;

    // Tax rates - dynamically loaded from backend
    public static double MWST = 7.7;
    public static double VERKAUFS_STEUER_1ZU1 = 26.0;
    public static double SHOP_STEUER = 2.0;
    public static double LAND_STEUER_BASIS_PER_BLOCK = 1.0;
    public static double[][] EINKOMMEN_STEUER_BRACKETS = {
            {0, 5},
            {1000000, 12},
            {5000000, 20},
            {10000000, 30}
    };
    public static double[][] VERMOEGENS_STEUER_BRACKETS = {
            {0, 0.2},
            {5000000, 0.5},
            {25000000, 1.0}
    };
    public static long ORE_CONVERT_TAX = 500;
    public static long COAL_ORE_CONVERSION = 50;
    public static long IRON_ORE_CONVERSION = 100;
    public static long COPPER_ORE_CONVERSION = 80;
    public static long GOLD_ORE_CONVERSION = 300;
    public static long REDSTONE_ORE_CONVERSION = 150;
    public static long LAPIS_ORE_CONVERSION = 200;
    public static long DIAMOND_ORE_CONVERSION = 1000;
    public static long EMERALD_ORE_CONVERSION = 1500;
    public static long QUARTZ_ORE_CONVERSION = 120;
    public static long ANCIENT_DEBRIS_CONVERSION = 2000;
    public static int LAND_STEUER_INTERVAL_DAYS = 3;

    public static void init(String apiBase, String apiKey) {
        API_BASE = apiBase;
        API_KEY = apiKey;
    }

    public static void loadConfigFromPlugin(Plugin plugin) {
        API_BASE = plugin.getConfig().getString("initiatives_api_base");
        API_KEY = plugin.getConfig().getString("minecraft_api_key");
        logger = plugin.getLogger();
        loadAllTaxConfigFromBackend();
    }

    /**
     * Loads all tax configuration from the backend.
     * This is called on plugin startup and can be called to refresh values.
     */
    public static void loadAllTaxConfigFromBackend() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/finances/tax-config/all"))
                    .GET()
                    .header("minecraft-api-key", API_KEY)
                    .build();

            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                JsonObject configs = GSON.fromJson(res.body(), JsonObject.class);
                
                // Load simple numeric values
                if (configs.has("MWST")) MWST = configs.get("MWST").getAsDouble();
                if (configs.has("VERKAUFS_STEUER_1ZU1")) VERKAUFS_STEUER_1ZU1 = configs.get("VERKAUFS_STEUER_1ZU1").getAsDouble();
                if (configs.has("SHOP_STEUER")) SHOP_STEUER = configs.get("SHOP_STEUER").getAsDouble();
                if (configs.has("LAND_STEUER_BASIS_PER_BLOCK")) LAND_STEUER_BASIS_PER_BLOCK = configs.get("LAND_STEUER_BASIS_PER_BLOCK").getAsDouble();
                if (configs.has("LAND_STEUER_INTERVAL_DAYS")) LAND_STEUER_INTERVAL_DAYS = configs.get("LAND_STEUER_INTERVAL_DAYS").getAsInt();
                if (configs.has("ORE_CONVERT_TAX")) ORE_CONVERT_TAX = configs.get("ORE_CONVERT_TAX").getAsLong();
                
                // Load ore conversion rates
                if (configs.has("COAL_ORE_CONVERSION")) COAL_ORE_CONVERSION = configs.get("COAL_ORE_CONVERSION").getAsLong();
                if (configs.has("IRON_ORE_CONVERSION")) IRON_ORE_CONVERSION = configs.get("IRON_ORE_CONVERSION").getAsLong();
                if (configs.has("COPPER_ORE_CONVERSION")) COPPER_ORE_CONVERSION = configs.get("COPPER_ORE_CONVERSION").getAsLong();
                if (configs.has("GOLD_ORE_CONVERSION")) GOLD_ORE_CONVERSION = configs.get("GOLD_ORE_CONVERSION").getAsLong();
                if (configs.has("REDSTONE_ORE_CONVERSION")) REDSTONE_ORE_CONVERSION = configs.get("REDSTONE_ORE_CONVERSION").getAsLong();
                if (configs.has("LAPIS_ORE_CONVERSION")) LAPIS_ORE_CONVERSION = configs.get("LAPIS_ORE_CONVERSION").getAsLong();
                if (configs.has("DIAMOND_ORE_CONVERSION")) DIAMOND_ORE_CONVERSION = configs.get("DIAMOND_ORE_CONVERSION").getAsLong();
                if (configs.has("EMERALD_ORE_CONVERSION")) EMERALD_ORE_CONVERSION = configs.get("EMERALD_ORE_CONVERSION").getAsLong();
                if (configs.has("QUARTZ_ORE_CONVERSION")) QUARTZ_ORE_CONVERSION = configs.get("QUARTZ_ORE_CONVERSION").getAsLong();
                if (configs.has("ANCIENT_DEBRIS_CONVERSION")) ANCIENT_DEBRIS_CONVERSION = configs.get("ANCIENT_DEBRIS_CONVERSION").getAsLong();
                
                // Load tax brackets
                if (configs.has("EINKOMMEN_STEUER_BRACKETS")) {
                    EINKOMMEN_STEUER_BRACKETS = parseTaxBrackets(configs.getAsJsonArray("EINKOMMEN_STEUER_BRACKETS"));
                }
                if (configs.has("VERMOEGENS_STEUER_BRACKETS")) {
                    VERMOEGENS_STEUER_BRACKETS = parseTaxBrackets(configs.getAsJsonArray("VERMOEGENS_STEUER_BRACKETS"));
                }
                
                if (logger != null) {
                    logger.info("[TaxRequests] All tax configuration loaded from backend");
                }
            } else {
                if (logger != null) {
                    logger.warning("[TaxRequests] Failed to load tax config: HTTP " + res.statusCode());
                }
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.severe("[TaxRequests] Error loading tax config: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    private static double[][] parseTaxBrackets(JsonArray array) {
        double[][] brackets = new double[array.size()][2];
        for (int i = 0; i < array.size(); i++) {
            JsonObject bracket = array.get(i).getAsJsonObject();
            brackets[i][0] = bracket.get("threshold").getAsDouble();
            brackets[i][1] = bracket.get("rate").getAsDouble();
        }
        return brackets;
    }

    /**
     * Update a single tax config value in the backend.
     */
    public static void updateTaxConfigInBackend(String configKey, Object value) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("value", value.toString());
            
            if (value instanceof Double || value instanceof Long) {
                payload.addProperty("type", "number");
            } else {
                payload.addProperty("type", "string");
            }

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/finances/tax-config/" + configKey))
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .header("Content-Type", "application/json")
                    .header("minecraft-api-key", API_KEY)
                    .build();

            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                if (logger != null) {
                    logger.info("[TaxRequests] Updated " + configKey + " to " + value);
                }
            } else {
                if (logger != null) {
                    logger.warning("[TaxRequests] Failed to update " + configKey + ": HTTP " + res.statusCode());
                }
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.severe("[TaxRequests] Error updating " + configKey + ": " + e.getMessage());
            }
        }
    }

    // Getters for tax rates
    public static double getMWST() {
        return MWST;
    }

    public static double getVerkaufsSteuer1zu1() {
        return VERKAUFS_STEUER_1ZU1;
    }

    public static double getShopSteuer() {
        return SHOP_STEUER;
    }

    public static double getLandSteuerBasisPerBlock() {
        return LAND_STEUER_BASIS_PER_BLOCK;
    }

    // Calculate progressive taxes
    public static long calculateEinkommenSteuer(long incomeCents) {
        long tax = 0;
        double prevThreshold = 0;
        for (double[] bracket : EINKOMMEN_STEUER_BRACKETS) {
            double threshold = bracket[0];
            double rate = bracket[1] / 100.0;
            if (incomeCents > threshold) {
                tax += (long) ((threshold - prevThreshold) * rate);
                prevThreshold = threshold;
            } else {
                tax += (long) ((incomeCents - prevThreshold) * rate);
                return tax;
            }
        }
        // Highest bracket
        double rate = EINKOMMEN_STEUER_BRACKETS[EINKOMMEN_STEUER_BRACKETS.length - 1][1] / 100.0;
        tax += (long) ((incomeCents - prevThreshold) * rate);
        return tax;
    }

    public static long calculateVermoegensSteuer(long wealthCents) {
        long tax = 0;
        double prevThreshold = 0;
        for (double[] bracket : VERMOEGENS_STEUER_BRACKETS) {
            double threshold = bracket[0];
            double rate = bracket[1] / 100.0;
            if (wealthCents > threshold) {
                tax += (long) ((threshold - prevThreshold) * rate);
                prevThreshold = threshold;
            } else {
                tax += (long) ((wealthCents - prevThreshold) * rate);
                return tax;
            }
        }
        // Highest bracket
        double rate = VERMOEGENS_STEUER_BRACKETS[VERMOEGENS_STEUER_BRACKETS.length - 1][1] / 100.0;
        tax += (long) ((wealthCents - prevThreshold) * rate);
        return tax;
    }

    // Setters for dynamic adjustments
    public static void setMWST(double rate) {
        MWST = Math.max(0, rate);
        updateTaxConfigInBackend("MWST", rate);
        if (logger != null) {
            logger.info("[TaxRequests] Set MWST to " + rate + "%");
        }
    }

    public static long getOreConvertTax() {
        return ORE_CONVERT_TAX;
    }

    public static void setOreConvertTax(long tax) {
        ORE_CONVERT_TAX = Math.max(0, tax);
        updateTaxConfigInBackend("ORE_CONVERT_TAX", tax);
        if (logger != null) {
            logger.info("[TaxRequests] ORE_CONVERT_TAX auf " + tax + " Cents gesetzt.");
        }
    }

    public static int getLandSteuerIntervalDays() {
        return Math.max(1, LAND_STEUER_INTERVAL_DAYS);
    }

    public static void setLandSteuerIntervalDays(int days) {
        LAND_STEUER_INTERVAL_DAYS = Math.max(1, days);
        updateTaxConfigInBackend("LAND_STEUER_INTERVAL_DAYS", days);
        if (logger != null) {
            logger.info("[TaxRequests] Landsteuer-Intervall gesetzt auf " + days + " Tage.");
        }
    }
}
