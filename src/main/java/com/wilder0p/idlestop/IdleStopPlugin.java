package com.wilder0p.idlestop;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class IdleStopPlugin extends JavaPlugin {

    private long startTimeMillis;
    private long emptySinceMillis = -1L; // -1 = not currently empty / countdown not running
    private boolean minUptimeLogged = false;
    private boolean stopTriggered = false;

    private long minUptimeMillis;
    private long emptyRequiredMillis;
    private int checkIntervalTicks;

    private BukkitTask checkTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();

        startTimeMillis = System.currentTimeMillis();
        getLogger().info("IdleStop enabled. Minimum uptime: " + (minUptimeMillis / 3_600_000L) + "h, empty requirement: "
                + (emptyRequiredMillis / 1000L) + "s.");

        checkTask = Bukkit.getScheduler().runTaskTimer(this, this::checkConditions, checkIntervalTicks, checkIntervalTicks);
    }

    @Override
    public void onDisable() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
    }

    private void reloadSettings() {
        reloadConfig();
        double hours = getConfig().getDouble("min-uptime-hours", 24.0);
        minUptimeMillis = (long) (hours * 3_600_000L);

        long seconds = getConfig().getLong("empty-seconds", 60L);
        emptyRequiredMillis = seconds * 1000L;

        checkIntervalTicks = Math.max(1, getConfig().getInt("check-interval-ticks", 20));
    }

    private void checkConditions() {
        if (stopTriggered) {
            return;
        }

        boolean empty = Bukkit.getOnlinePlayers().isEmpty();

        if (!empty) {
            if (emptySinceMillis != -1L) {
                getLogger().info("Player(s) online again \u2014 empty countdown cancelled.");
                emptySinceMillis = -1L;
            }
            return;
        }

        // Server is empty
        long now = System.currentTimeMillis();

        if (emptySinceMillis == -1L) {
            emptySinceMillis = now;
            getLogger().info("Server is empty. Starting " + (emptyRequiredMillis / 1000L) + "s countdown...");
        }

        long emptyDuration = now - emptySinceMillis;
        if (emptyDuration < emptyRequiredMillis) {
            return;
        }

        // Empty long enough \u2014 now check uptime
        long uptime = now - startTimeMillis;
        if (uptime < minUptimeMillis) {
            if (!minUptimeLogged) {
                long remainingMs = minUptimeMillis - uptime;
                long remainingMin = (remainingMs + 59_999L) / 60_000L;
                getLogger().info("Empty for required duration, but minimum uptime not yet reached. "
                        + "Approx. " + remainingMin + " minute(s) remaining until uptime condition is met.");
                minUptimeLogged = true;
            }
            return;
        }

        if (!minUptimeLogged) {
            getLogger().info("Minimum uptime of " + (minUptimeMillis / 3_600_000L) + "h reached.");
            minUptimeLogged = true;
        }

        // Both conditions true
        stopTriggered = true;
        getLogger().info("Both conditions met (uptime >= " + (minUptimeMillis / 3_600_000L)
                + "h and empty for >= " + (emptyRequiredMillis / 1000L)
                + "s). Executing /stop...");

        // Run on next tick so the log is flushed cleanly
        Bukkit.getScheduler().runTask(this, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stop");
        });
    }
}
