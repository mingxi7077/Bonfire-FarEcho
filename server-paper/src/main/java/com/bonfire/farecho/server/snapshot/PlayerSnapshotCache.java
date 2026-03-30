package com.bonfire.farecho.server.snapshot;

import com.bonfire.farecho.protocol.model.EchoFlags;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scoreboard.Team;

public final class PlayerSnapshotCache implements SnapshotProvider, Listener {
    private static final double MOVE_THRESHOLD_SQUARED = 0.25D;

    private final Map<UUID, PlayerSnapshot> snapshots = new ConcurrentHashMap<>();

    @Override
    public Collection<PlayerSnapshot> snapshots() {
        return snapshots.values();
    }

    @Override
    public Optional<PlayerSnapshot> snapshot(UUID uuid) {
        return Optional.ofNullable(snapshots.get(uuid));
    }

    @Override
    public void refresh(Player player) {
        snapshots.put(player.getUniqueId(), capture(player));
    }

    public void refreshAllOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    @Override
    public void remove(UUID playerId) {
        snapshots.remove(playerId);
    }

    @Override
    public int size() {
        return snapshots.size();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().distanceSquared(event.getTo()) >= MOVE_THRESHOLD_SQUARED) {
            refresh(event.getPlayer());
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        refresh(event.getPlayer());
    }

    private PlayerSnapshot capture(Player player) {
        int flags = 0;
        if (player.isInvisible()) {
            flags |= EchoFlags.INVISIBLE;
        }
        if (player.isSneaking()) {
            flags |= EchoFlags.SNEAKING;
        }
        if (player.isFlying()) {
            flags |= EchoFlags.FLYING;
        }
        if (player.getGameMode() == GameMode.SPECTATOR) {
            flags |= EchoFlags.SPECTATOR;
        }
        if (player.hasPermission("farecho.hidden")) {
            flags |= EchoFlags.ADMIN_HIDDEN;
        }

        Team team = player.getScoreboard().getEntryTeam(player.getName());
        String teamName = team == null ? "" : team.getName();

        return new PlayerSnapshot(
            player.getUniqueId(),
            player.getName(),
            player.getWorld().getKey().toString(),
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getLocation().getYaw(),
            player.getLocation().getPitch(),
            System.currentTimeMillis() / 50L,
            flags,
            teamName
        );
    }
}
