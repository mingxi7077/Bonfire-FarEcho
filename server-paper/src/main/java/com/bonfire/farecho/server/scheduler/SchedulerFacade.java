package com.bonfire.farecho.server.scheduler;

import org.bukkit.scheduler.BukkitTask;

public interface SchedulerFacade {
    BukkitTask runRepeating(Runnable task, long delayTicks, long periodTicks);

    void cancel(BukkitTask task);
}
