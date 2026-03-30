package com.bonfire.farecho.server.interest;

import com.bonfire.farecho.server.config.FarechoConfiguration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public final class RecentPrioritySignalCache implements PrioritySignalProvider, Listener {
    private final ConcurrentMap<UUID, ConcurrentMap<UUID, Long>> recentCombat = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ConcurrentMap<UUID, Long>> recentInteraction = new ConcurrentHashMap<>();
    private final Supplier<FarechoConfiguration> configurationSupplier;

    public RecentPrioritySignalCache(Supplier<FarechoConfiguration> configurationSupplier) {
        this.configurationSupplier = configurationSupplier;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player attacker)) {
            return;
        }
        markReciprocal(recentCombat, attacker.getUniqueId(), victim.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        Entity rightClicked = event.getRightClicked();
        if (!(rightClicked instanceof Player target)) {
            return;
        }
        markReciprocal(recentInteraction, event.getPlayer().getUniqueId(), target.getUniqueId(), System.currentTimeMillis());
    }

    @Override
    public boolean hasRecentCombat(UUID observerId, UUID targetId, long nowMs, FarechoConfiguration configuration) {
        return isRecent(recentCombat, observerId, targetId, nowMs, configuration.recentCombatWindowMs());
    }

    @Override
    public boolean hasRecentInteraction(UUID observerId, UUID targetId, long nowMs, FarechoConfiguration configuration) {
        return isRecent(recentInteraction, observerId, targetId, nowMs, configuration.recentInteractionWindowMs());
    }

    @Override
    public String traceFor(UUID playerId, long nowMs, FarechoConfiguration configuration) {
        int combat = activeTargetsFor(recentCombat, playerId, nowMs, configuration.recentCombatWindowMs());
        int interaction = activeTargetsFor(recentInteraction, playerId, nowMs, configuration.recentInteractionWindowMs());
        return "combat=" + combat + ", interaction=" + interaction;
    }

    @Override
    public void clear(UUID playerId) {
        recentCombat.remove(playerId);
        recentInteraction.remove(playerId);
        removeReverseLinks(recentCombat, playerId);
        removeReverseLinks(recentInteraction, playerId);
    }

    @Override
    public long activeCombatLinks(long nowMs, FarechoConfiguration configuration) {
        return countLinks(recentCombat, nowMs, configuration.recentCombatWindowMs());
    }

    @Override
    public long activeInteractionLinks(long nowMs, FarechoConfiguration configuration) {
        return countLinks(recentInteraction, nowMs, configuration.recentInteractionWindowMs());
    }

    private boolean isRecent(
        ConcurrentMap<UUID, ConcurrentMap<UUID, Long>> source,
        UUID observerId,
        UUID targetId,
        long nowMs,
        long windowMs
    ) {
        ConcurrentMap<UUID, Long> targets = source.get(observerId);
        if (targets == null) {
            return false;
        }
        Long timestamp = targets.get(targetId);
        if (timestamp == null) {
            return false;
        }
        if (nowMs - timestamp > windowMs) {
            targets.remove(targetId);
            if (targets.isEmpty()) {
                source.remove(observerId);
            }
            return false;
        }
        return true;
    }

    private int activeTargetsFor(
        ConcurrentMap<UUID, ConcurrentMap<UUID, Long>> source,
        UUID observerId,
        long nowMs,
        long windowMs
    ) {
        ConcurrentMap<UUID, Long> targets = source.get(observerId);
        if (targets == null) {
            return 0;
        }
        targets.entrySet().removeIf(entry -> nowMs - entry.getValue() > windowMs);
        if (targets.isEmpty()) {
            source.remove(observerId);
            return 0;
        }
        return targets.size();
    }

    private long countLinks(ConcurrentMap<UUID, ConcurrentMap<UUID, Long>> source, long nowMs, long windowMs) {
        long count = 0;
        for (Map.Entry<UUID, ConcurrentMap<UUID, Long>> observerEntry : source.entrySet()) {
            ConcurrentMap<UUID, Long> targets = observerEntry.getValue();
            targets.entrySet().removeIf(targetEntry -> nowMs - targetEntry.getValue() > windowMs);
            if (targets.isEmpty()) {
                source.remove(observerEntry.getKey());
                continue;
            }
            count += targets.size();
        }
        return count;
    }

    private void markReciprocal(
        ConcurrentMap<UUID, ConcurrentMap<UUID, Long>> source,
        UUID playerA,
        UUID playerB,
        long timestampMs
    ) {
        source.computeIfAbsent(playerA, key -> new ConcurrentHashMap<>()).put(playerB, timestampMs);
        source.computeIfAbsent(playerB, key -> new ConcurrentHashMap<>()).put(playerA, timestampMs);
    }

    private void removeReverseLinks(ConcurrentMap<UUID, ConcurrentMap<UUID, Long>> source, UUID playerId) {
        for (Map.Entry<UUID, ConcurrentMap<UUID, Long>> entry : source.entrySet()) {
            entry.getValue().remove(playerId);
            if (entry.getValue().isEmpty()) {
                source.remove(entry.getKey());
            }
        }
    }
}
