package com.HelvetiCraft.requests;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

/**
 * Dummy backend request logger for admin upgrades/downgrades.
 */
public class AdminRequests {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static Logger logger;

    public static void init(JavaPlugin plugin) {
        logger = plugin.getLogger();
    }

    public static void logUpgrade(UUID playerId, String playerName, String reason, long expiresAtMillis) {
        String expires = DF.format(Instant.ofEpochMilli(expiresAtMillis));
        logger.info("[AdminRequests] Upgrade: player=" + playerName + " (" + playerId + ") reason='" + reason + "' expires=" + expires);
    }

    public static void logDowngrade(UUID playerId, String playerName, String reason) {
        String now = DF.format(Instant.now());
        logger.info("[AdminRequests] Downgrade: player=" + playerName + " (" + playerId + ") reason='" + reason + "' at=" + now);
    }
}
