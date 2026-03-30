package com.bonfire.farecho.server.interest;

import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

public record ObserverContext(
    UUID uuid,
    String worldKey,
    double x,
    double y,
    double z,
    String teamName,
    boolean canSeeInvisible,
    boolean canSeeHidden,
    boolean spectator,
    boolean pvpEnabled
) {
    public static ObserverContext fromPlayer(Player player) {
        Team team = player.getScoreboard().getEntryTeam(player.getName());
        String teamName = team == null ? "" : team.getName();
        return new ObserverContext(
            player.getUniqueId(),
            player.getWorld().getKey().toString(),
            player.getX(),
            player.getY(),
            player.getZ(),
            teamName,
            player.hasPermission("farecho.see.invisible"),
            player.hasPermission("farecho.see.hidden"),
            player.getGameMode() == GameMode.SPECTATOR,
            player.getWorld().getPVP()
        );
    }
}
