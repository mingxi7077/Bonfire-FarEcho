package com.bonfire.farecho.server.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class BukkitSchedulerFacade implements SchedulerFacade {
    private final JavaPlugin plugin;

    public BukkitSchedulerFacade(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public BukkitTask runRepeating(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
    }

    @Override
    public void cancel(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }
}
