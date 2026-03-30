package com.bonfire.farecho.server;

import com.bonfire.farecho.protocol.ProtocolRegistry;
import com.bonfire.farecho.server.broadcast.BroadcastPolicy;
import com.bonfire.farecho.server.broadcast.DistanceTierBroadcastPolicy;
import com.bonfire.farecho.server.broadcast.EchoBroadcastService;
import com.bonfire.farecho.server.config.FarechoConfiguration;
import com.bonfire.farecho.server.interest.DefaultInterestEngine;
import com.bonfire.farecho.server.interest.InterestEngine;
import com.bonfire.farecho.server.interest.RecentPrioritySignalCache;
import com.bonfire.farecho.server.metrics.MetricsCollector;
import com.bonfire.farecho.server.metrics.MetricsSnapshot;
import com.bonfire.farecho.server.scheduler.BukkitSchedulerFacade;
import com.bonfire.farecho.server.scheduler.SchedulerFacade;
import com.bonfire.farecho.server.snapshot.PlayerSnapshotCache;
import com.bonfire.farecho.server.transport.EchoTransport;
import com.bonfire.farecho.server.transport.HandshakeService;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class FarEchoServerPlugin extends JavaPlugin implements Listener {
    private final AtomicReference<FarechoConfiguration> configurationRef = new AtomicReference<>();

    private SchedulerFacade scheduler;
    private PlayerSnapshotCache snapshotCache;
    private InterestEngine interestEngine;
    private RecentPrioritySignalCache prioritySignalCache;
    private BroadcastPolicy broadcastPolicy;
    private MetricsCollector metricsCollector;
    private EchoTransport transport;
    private HandshakeService handshakeService;
    private EchoBroadcastService broadcastService;

    private BukkitTask snapshotTask;
    private BukkitTask broadcastTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadRuntimeConfiguration();

        scheduler = new BukkitSchedulerFacade(this);
        snapshotCache = new PlayerSnapshotCache();
        prioritySignalCache = new RecentPrioritySignalCache(configurationRef::get);
        interestEngine = new DefaultInterestEngine(prioritySignalCache);
        broadcastPolicy = new DistanceTierBroadcastPolicy();
        metricsCollector = new MetricsCollector();

        ProtocolRegistry protocolRegistry = ProtocolRegistry.createDefault();
        transport = new EchoTransport(this, protocolRegistry, (player, channel, frame) -> handshakeService.handleInbound(player, channel, frame));
        handshakeService = new HandshakeService(this, transport, configurationRef::get);
        broadcastService = new EchoBroadcastService(
            snapshotCache,
            interestEngine,
            broadcastPolicy,
            handshakeService,
            transport,
            metricsCollector,
            configurationRef::get
        );

        transport.registerChannels();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(snapshotCache, this);
        getServer().getPluginManager().registerEvents(prioritySignalCache, this);

        snapshotCache.refreshAllOnline();
        snapshotTask = scheduler.runRepeating(snapshotCache::refreshAllOnline, 20L, 20L);
        broadcastTask = scheduler.runRepeating(broadcastService, 5L, 5L);

        if (getCommand("farecho") != null) {
            getCommand("farecho").setExecutor(new FarechoCommand());
            getCommand("farecho").setTabCompleter(new FarechoCommand());
        }

        getLogger().info("FarEcho enabled. protocol=" + com.bonfire.farecho.protocol.ProtocolVersion.CURRENT);
    }

    @Override
    public void onDisable() {
        scheduler.cancel(snapshotTask);
        scheduler.cancel(broadcastTask);
        if (transport != null) {
            transport.unregisterChannels();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        handshakeService.forget(event.getPlayer().getUniqueId());
        broadcastService.forget(event.getPlayer().getUniqueId());
        prioritySignalCache.clear(event.getPlayer().getUniqueId());
    }

    private void reloadRuntimeConfiguration() {
        reloadConfig();
        configurationRef.set(FarechoConfiguration.from(getConfig()));
    }

    private final class FarechoCommand implements TabExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length == 0) {
                sender.sendMessage("/farecho <stats|reload|trace>");
                return true;
            }

            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "stats" -> {
                    MetricsSnapshot snapshot = metricsCollector.snapshot();
                    sender.sendMessage("FarEcho stats(total): cycles=" + snapshot.cycles()
                        + ", targets=" + snapshot.targets()
                        + ", clippedRatio=" + String.format("%.3f", snapshot.clippedRatio())
                        + ", packets=" + snapshot.packets()
                        + ", degraded=" + snapshot.degraded()
                        + ", bytes=" + snapshot.bytes());
                    sender.sendMessage("FarEcho stats(1s): targets/s=" + snapshot.targetsPerSecond()
                        + ", clippedRatio=" + String.format("%.3f", snapshot.clippedRatioPerSecond())
                        + ", packets/s=" + snapshot.packetsPerSecond()
                        + ", degraded/s=" + snapshot.degradedPerSecond()
                        + ", bytes/s=" + snapshot.bytesPerSecond()
                        + ", render/s=" + snapshot.clientRenderCountPerSecond());
                    long nowMs = System.currentTimeMillis();
                    FarechoConfiguration configuration = configurationRef.get();
                    sender.sendMessage("Known clients=" + handshakeService.knownClientCount()
                        + ", snapshots=" + snapshotCache.size()
                        + ", combatLinks=" + prioritySignalCache.activeCombatLinks(nowMs, configuration)
                        + ", interactionLinks=" + prioritySignalCache.activeInteractionLinks(nowMs, configuration));
                    return true;
                }
                case "reload" -> {
                    if (!sender.hasPermission("farecho.admin")) {
                        sender.sendMessage("No permission.");
                        return true;
                    }
                    reloadRuntimeConfiguration();
                    sender.sendMessage("FarEcho config reloaded.");
                    return true;
                }
                case "trace" -> {
                    if (args.length < 2) {
                        sender.sendMessage("Usage: /farecho trace <player>");
                        return true;
                    }
                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        sender.sendMessage("Player not found.");
                        return true;
                    }
                    sender.sendMessage("trace(" + target.getName() + "): client=" + handshakeService.isFarEchoClient(target)
                        + ", " + broadcastService.trace(target.getUniqueId()));
                    sender.sendMessage("signals(" + target.getName() + "): "
                        + prioritySignalCache.traceFor(target.getUniqueId(), System.currentTimeMillis(), configurationRef.get()));
                    return true;
                }
                default -> {
                    sender.sendMessage("Unknown subcommand.");
                    return true;
                }
            }
        }

        @Override
        public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) {
                return java.util.List.of("stats", "reload", "trace");
            }
            if (args.length == 2 && "trace".equalsIgnoreCase(args[0])) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            }
            return java.util.List.of();
        }
    }
}
