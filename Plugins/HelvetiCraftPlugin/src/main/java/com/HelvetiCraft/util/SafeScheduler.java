package com.HelvetiCraft.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

public final class SafeScheduler {

    private static final boolean FOLIA_AVAILABLE;
    private static final Method GET_REGION_SCHEDULER;
    private static final Method RS_RUN_AT_LOCATION; // reflection method placeholder
    private static final Method RS_RUN_AT_LOCATION_DELAYED;

    static {
        boolean folia = false;
        Method getRegionScheduler = null;
        Method runAtLocation = null;
        Method runAtLocationDelayed = null;

        try {
            // Detect Folia / Paper RegionScheduler
            // we try to get org.bukkit.Server#getRegionScheduler()
            getRegionScheduler = Bukkit.getServer().getClass().getMethod("getRegionScheduler");
            Object rs = getRegionScheduler.invoke(Bukkit.getServer());
            if (rs != null) {
                // Attempt to find runAtLocation(Plugin, Location, Runnable)
                // Actual Folia method names can differ between versions — reflection attempt:
                for (Method m : rs.getClass().getMethods()) {
                    if (m.getName().toLowerCase().contains("run") && m.getParameterCount() >= 3) {
                        // common signature: runAtLocation(Plugin, Location, Runnable)
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length >= 3 && params[0].getName().contains("Plugin") && params[1].getName().contains("Location")) {
                            runAtLocation = m;
                        }
                        // also detect delayed variant with long param maybe at end
                        if (params.length >= 4 && params[0].getName().contains("Plugin") && params[1].getName().contains("Location")) {
                            runAtLocationDelayed = m;
                        }
                    }
                }
                folia = true;
            }
        } catch (Throwable ignored) {
            folia = false;
        }

        FOLIA_AVAILABLE = folia;
        GET_REGION_SCHEDULER = getRegionScheduler;
        RS_RUN_AT_LOCATION = runAtLocation;
        RS_RUN_AT_LOCATION_DELAYED = runAtLocationDelayed;
    }

    private SafeScheduler() {}

    // Run sync on the main/server thread (fallback)
    public static BukkitTask runSync(Plugin plugin, Runnable task) {
        return Bukkit.getScheduler().runTask(plugin, task);
    }

    // Run asynchronously (background)
    public static BukkitTask runAsync(Plugin plugin, Runnable task) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    // Run after delay ticks on main thread
    public static BukkitTask runLater(Plugin plugin, Runnable task, long delayTicks) {
        return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    // Run repeating on main thread
    public static BukkitTask runRepeating(Plugin plugin, Runnable task, long initialDelay, long period) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, initialDelay, period);
    }

    // Try to run at the location's region thread (Folia) if available; fallback to runSync
    public static void runAtLocation(Plugin plugin, org.bukkit.Location loc, Runnable task) {
        if (FOLIA_AVAILABLE && GET_REGION_SCHEDULER != null && RS_RUN_AT_LOCATION != null) {
            try {
                Object regionScheduler = GET_REGION_SCHEDULER.invoke(Bukkit.getServer());
                // The reflection call might require slightly different parameters across versions.
                // Try common signature: (Plugin, Location, Runnable)
                try {
                    RS_RUN_AT_LOCATION.invoke(regionScheduler, plugin, loc, task);
                    return;
                } catch (IllegalArgumentException ignored) {
                    // maybe different signature, try with long delay param 0
                    if (RS_RUN_AT_LOCATION_DELAYED != null) {
                        try {
                            RS_RUN_AT_LOCATION_DELAYED.invoke(regionScheduler, plugin, loc, task, 0L);
                            return;
                        } catch (Throwable ignored2) {
                        }
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                // fall through to fallback
            } catch (Throwable ignored) {}
        }
        // fallback
        runSync(plugin, task);
    }

    // Helper: schedule repeating at location — same concept, omitted here for brevity
    public static void runRepeatingAtLocation(Plugin plugin, org.bukkit.Location loc, Runnable task, long initialDelay, long period) {
        // If Folia available, you'd want to call the region scheduler repeating method (reflection).
        // For now fallback to runRepeating on main thread.
        runRepeating(plugin, task, initialDelay, period);
    }

    public static boolean isFoliaAvailable() {
        return FOLIA_AVAILABLE;
    }
}
