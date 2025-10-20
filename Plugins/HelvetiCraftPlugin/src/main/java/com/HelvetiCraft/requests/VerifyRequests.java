package com.HelvetiCraft.requests;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VerifyRequests {

    private static final Map<UUID, String> codes = new ConcurrentHashMap<>();

    public static String generateCode(UUID playerUUID) {
        // Generate a 6-character alphanumeric code
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int idx = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(idx));
        }
        String code = sb.toString();
        codes.put(playerUUID, code);
        System.out.println("[VerifyRequests] Generated code " + code + " for " + playerUUID);
        return code;
    }

    public static void markVerified(UUID playerUUID) {
        codes.remove(playerUUID);
        System.out.println("[VerifyRequests] Player " + playerUUID + " marked as verified.");
    }

    public static boolean verify(UUID playerUUID, String code) {
        String stored = codes.get(playerUUID);
        if (stored != null && stored.equalsIgnoreCase(code)) {
            markVerified(playerUUID);
            return true;
        }
        return false;
    }
}
