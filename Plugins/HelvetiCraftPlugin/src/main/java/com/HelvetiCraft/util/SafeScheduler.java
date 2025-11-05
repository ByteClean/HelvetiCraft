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
    private static final Method GET_GLOBAL_REGION_SCHEDULER;
    private static final Method RS_RUN_DELAYED;

    static {
        boolean folia = false;
        Method getRegionScheduler = null;
        Method getGlobalRegionScheduler = null;
        Method runDelayed = null;

        try {
            // Detect Folia
            Class<?> serverClass = Bukkit.getServer().getClass();
            if (serverClass.getName().contains("Folia")) {
                getRegionScheduler = serverClass.getMethod("getRegionScheduler");
                getGlobalRegionScheduler = serverClass.getMethod("getGlobalRegionScheduler");
                Object grs = getGlobalRegionScheduler.invoke(Bukkit.getServer());
                
                // Find the delayed execution method
                for (Method m : grs.getClass().getMethods()) {
                    if (m.getName().equals("runDelayed")) {
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length == 3 
                            && params[0].getName().contains("Plugin")
                            && params[1].getName().contains("Runnable")
                            && params[2] == long.class) {
                            runDelayed = m;
                            break;
                        }
                    }
                }
                if (runDelayed != null) {
                    folia = true;
                }
            }
        } catch (Throwable ignored) {
            folia = false;
        }

        FOLIA_AVAILABLE = folia;
        GET_REGION_SCHEDULER = getRegionScheduler;
        GET_GLOBAL_REGION_SCHEDULER = getGlobalRegionScheduler;
        RS_RUN_DELAYED = runDelayed;
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

    // Run after delay ticks
    public static BukkitTask runLater(Plugin plugin, Runnable task, long delayTicks) {
        if (FOLIA_AVAILABLE && GET_GLOBAL_REGION_SCHEDULER != null && RS_RUN_DELAYED != null) {
            try {
                Object scheduler = GET_GLOBAL_REGION_SCHEDULER.invoke(Bukkit.getServer());
                RS_RUN_DELAYED.invoke(scheduler, plugin, task, delayTicks);
                return null; // Folia doesn't return BukkitTask
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    // Run repeating - Note: In Folia, you'd need to schedule the next run inside the task
    public static BukkitTask runRepeating(Plugin plugin, Runnable task, long initialDelay, long period) {
        if (FOLIA_AVAILABLE) {
            runLater(plugin, new Runnable() {
                @Override
                public void run() {
                    task.run();
                    runLater(plugin, this, period);
                }
            }, initialDelay);
            return null;
        }
        return Bukkit.getScheduler().runTaskTimer(plugin, task, initialDelay, period);
    }

    // Try to run at the location's region thread (Folia) if available; fallback to runSync
    public static void runAtLocation(Plugin plugin, org.bukkit.Location loc, Runnable task) {
        if (FOLIA_AVAILABLE && GET_REGION_SCHEDULER != null) {
            try {
                Object regionScheduler = GET_REGION_SCHEDULER.invoke(Bukkit.getServer());
                // Use global scheduler as fallback
                if (GET_GLOBAL_REGION_SCHEDULER != null) {
                    Object globalScheduler = GET_GLOBAL_REGION_SCHEDULER.invoke(Bukkit.getServer());
                    if (RS_RUN_DELAYED != null) {
                        RS_RUN_DELAYED.invoke(globalScheduler, plugin, task, 0L);
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // fallback to sync
        runSync(plugin, task);
    }

    public static boolean isFoliaAvailable() {
        return FOLIA_AVAILABLE;
    }
}
