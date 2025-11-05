package com.HelvetiCraft.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

/**
 * Safe scheduler utility for both Paper and Folia.
 * Provides unified scheduling and cancellation support.
 */
public final class SafeScheduler {

    private static final boolean FOLIA = detectFolia();

    private SafeScheduler() {}

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** Unified task handle for both BukkitTask and ScheduledTask */
    public interface SafeTask {
        void cancel();
        boolean isCancelled();
    }

    private static SafeTask wrapBukkitTask(BukkitTask task) {
        return new SafeTask() {
            @Override public void cancel() { task.cancel(); }
            @Override public boolean isCancelled() { return task.isCancelled(); }
        };
    }

    private static SafeTask wrapFoliaTask(ScheduledTask task) {
        return new SafeTask() {
            @Override public void cancel() { task.cancel(); }
            @Override public boolean isCancelled() { return task.isCancelled(); }
        };
    }

    /** Run sync immediately */
    public static SafeTask runSync(Plugin plugin, Runnable task) {
        if (FOLIA) {
            ScheduledTask t = Bukkit.getGlobalRegionScheduler().run(plugin, s -> task.run());
            return wrapFoliaTask(t);
        }
        return wrapBukkitTask(Bukkit.getScheduler().runTask(plugin, task));
    }

    /** Run asynchronously */
    public static SafeTask runAsync(Plugin plugin, Runnable task) {
        if (FOLIA) {
            ScheduledTask t = Bukkit.getAsyncScheduler().runNow(plugin, s -> task.run());
            return wrapFoliaTask(t);
        }
        return wrapBukkitTask(Bukkit.getScheduler().runTaskAsynchronously(plugin, task));
    }

    /** Run after delay */
    public static SafeTask runLater(Plugin plugin, Runnable task, long delayTicks) {
        if (FOLIA) {
            ScheduledTask t = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, s -> task.run(), delayTicks);
            return wrapFoliaTask(t);
        }
        return wrapBukkitTask(Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks));
    }

    /** Run repeating */
    public static SafeTask runRepeating(Plugin plugin, Runnable task, long initialDelay, long period) {
        if (FOLIA) {
            ScheduledTask t = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, s -> task.run(), initialDelay, period);
            return wrapFoliaTask(t);
        }
        return wrapBukkitTask(Bukkit.getScheduler().runTaskTimer(plugin, task, initialDelay, period));
    }

    /** Run task safely at a specific location */
    public static void runAtLocation(Plugin plugin, Location loc, Runnable task) {
        if (FOLIA) {
            Bukkit.getRegionScheduler().execute(plugin, loc, task);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static boolean isFoliaAvailable() {
        return FOLIA;
    }
}
