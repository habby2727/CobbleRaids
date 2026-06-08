package com.kingpixel.cobbleraids;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kingpixel.cobbleraids.command.CommandTree;
import com.kingpixel.cobbleraids.config.CategoryConfig;
import com.kingpixel.cobbleraids.config.Config;
import com.kingpixel.cobbleraids.config.Lang;
import com.kingpixel.cobbleraids.config.RaidConfigs;
import com.kingpixel.cobbleraids.database.DataBaseClient;
import com.kingpixel.cobbleraids.database.DataBaseFactory;
import com.kingpixel.cobbleraids.events.cobblemon.*;
import com.kingpixel.cobbleraids.events.raids.RaidEvents;
import com.kingpixel.cobbleraids.manager.CaptureSessionManager;
import com.kingpixel.cobbleraids.manager.RaidHistory;
import com.kingpixel.cobbleraids.manager.RaidManager;
import com.kingpixel.cobbleraids.manager.RewardsManager;
import com.kingpixel.cobbleraids.models.RaidBall;
import com.kingpixel.cobbleutils.util.UtilsLogger;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.Logger;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CobbleRaids {
  public static final String MOD_ID = "cobbleraids";
  public static final String MOD_NAME = "CobbleRaids";
  public static final String PATH = "/config/" + MOD_ID;
  public static final String PATH_LANG = PATH + "/lang/";
  public static final Logger LOGGER = UtilsLogger.getLogger(MOD_ID);
  public static MinecraftServer server;
  public static Config config = new Config();
  public static Lang language = new Lang();
  public static RaidConfigs raidConfigs = new RaidConfigs();
  public static CategoryConfig categorys = new CategoryConfig();
  public static int oldLevelCap = Cobblemon.INSTANCE.getConfig().getMaxPokemonLevel();
  public static RaidManager raidManager = new RaidManager();
  public static RewardsManager rewardsManager = new RewardsManager();
  public static CaptureSessionManager captureSessionManager = new CaptureSessionManager();
  public static RaidHistory raidHistory = new RaidHistory();
  public static final Executor COBBLE_RAID_EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
    .setNameFormat("Cobble-Raids-executor-%d")
    .setDaemon(true)
    .build());
  private static final ScheduledExecutorService COBBLE_RAIDS_SCHEDULER =
    Executors.newScheduledThreadPool(4, new ThreadFactoryBuilder()
      .setNameFormat("Cobble-Raids-scheduler-%d")
      .setDaemon(true)
      .build()
    );
  private static final AtomicBoolean TELEPORT_RAID_PLAYERS_TASK_QUEUED = new AtomicBoolean();
  private static final AtomicBoolean BOSS_BAR_TASK_QUEUED = new AtomicBoolean();
  private static final AtomicBoolean RAID_TICK_TASK_QUEUED = new AtomicBoolean();
  private static final AtomicBoolean CAPTURE_SESSION_TASK_QUEUED = new AtomicBoolean();
  private static final AtomicBoolean RANDOM_RAID_TASK_QUEUED = new AtomicBoolean();
  private static final AtomicBoolean RECONNECT_FIGHT_CHECK_TASK_QUEUED = new AtomicBoolean();

  public static void init() {
    events();
    files();
    tasks();
  }

  private static void tasks() {
    // Task to teleport out players that are fighting but are no longer in the raid area.
    COBBLE_RAIDS_SCHEDULER.scheduleWithFixedDelay(() ->
      runOnServerThread("TeleportRaidPlayersTask", TELEPORT_RAID_PLAYERS_TASK_QUEUED, () -> {
        if (raidManager == null) return;
        var fights = raidManager.getFightingByBattleUUID();
        if (fights == null || fights.isEmpty()) return;
        fights.forEach((key, value) -> {
          var player = value.getPlayer();
          var raid = value.getRaid();
          if (player == null || raid == null) return;
          raid.teleportPlayerOut(player);
        });
      }), 0, 50, TimeUnit.MILLISECONDS
    );

    // Task to update the boss bar of all active raids.
    COBBLE_RAIDS_SCHEDULER.scheduleWithFixedDelay(() -> {
      runOnServerThread("BossBarTask", BOSS_BAR_TASK_QUEUED, () -> {
        if (raidManager == null) return;
        var activeRaids = raidManager.getActiveRaids();
        if (activeRaids == null || activeRaids.isEmpty()) return;
        activeRaids.forEach((key, value) -> value.getCategoryRaid().manageBossBar(value));
      });
    }, 0, 1, TimeUnit.SECONDS);

    // Task to finish raids that finish by time.
    COBBLE_RAIDS_SCHEDULER.scheduleWithFixedDelay(() -> {
      runOnServerThread("RaidTickTask", RAID_TICK_TASK_QUEUED, () -> {
        if (raidManager == null) return;
        var activeRaids = raidManager.getActiveRaids();
        if (activeRaids == null || activeRaids.isEmpty()) return;
        activeRaids.forEach((key, value) -> {
          value.sendActionBarTimeLeft();
          if (value.isFinishByTime()) {
            value.finishRaid();
          }
        });
      });
    }, 0, 1, TimeUnit.SECONDS);
    // Task Capture session timeout
    COBBLE_RAIDS_SCHEDULER.scheduleWithFixedDelay(() -> {
      runOnServerThread("CaptureSessionTask", CAPTURE_SESSION_TASK_QUEUED, () -> {
        if (captureSessionManager == null) return;
        var sessions = captureSessionManager.getActiveSessions();
        if (sessions == null || sessions.isEmpty()) return;
        for (var entry : sessions.entrySet()) {
          var session = entry.getValue();
          session.checkTimeout();
        }
      });
    }, 0, 1, TimeUnit.SECONDS);

    // Task to Init a random raid if no raid is active.
    COBBLE_RAIDS_SCHEDULER.scheduleWithFixedDelay(() -> {
      runOnServerThread("RandomRaidTask", RANDOM_RAID_TASK_QUEUED, () -> {
        if (raidManager == null) return;
        if (!raidManager.isRandomRaidOn()) {
          raidManager.initRandomRaid();
        }
      });
    }, 0, 1, TimeUnit.SECONDS);

    // Task to check if the player continue in a battle if the player disconnect and reconnect.
    COBBLE_RAIDS_SCHEDULER.scheduleWithFixedDelay(() -> {
      runOnServerThread("ReconnectFightCheckTask", RECONNECT_FIGHT_CHECK_TASK_QUEUED, () -> {
        if (raidManager == null) return;
        var entries = raidManager.getFightingByPlayers().entrySet();
        if (entries.isEmpty()) return;
        for (var entry : entries) {
          var fightData = entry.getValue();
          var player = fightData.getPlayer();
          if (player == null) {
            fightData.stop(true);
            continue;
          }
          var battle = BattleRegistry.getBattleByParticipatingPlayer(player);
          if (battle == null) fightData.stop(true);
        }
      });
    }, 0, 10, TimeUnit.SECONDS);
  }

  private static void runOnServerThread(String taskName, AtomicBoolean queued, Runnable runnable) {
    var currentServer = server;
    if (currentServer == null) return;
    if (!queued.compareAndSet(false, true)) return;
    try {
      currentServer.execute(() -> {
        try {
          runnable.run();
        } catch (Exception e) {
          LOGGER.error("[" + taskName + "] Error: " + e.getMessage(), e);
        } finally {
          queued.set(false);
        }
      });
    } catch (RuntimeException e) {
      queued.set(false);
      LOGGER.error("[" + taskName + "] Could not enqueue task: " + e.getMessage(), e);
    }
  }

  public static boolean stopBattleSafely(UUID battleUUID, String reason) {
    if (battleUUID == null) return false;
    var battle = BattleRegistry.getBattle(battleUUID);
    if (battle == null) return false;
    try {
      if (config.isDebug()) {
        LOGGER.info("Stopping battle " + battleUUID + " reason=" + reason + " thread=" + Thread.currentThread().getName());
      }
      battle.stop();
      return true;
    } catch (Throwable throwable) {
      LOGGER.error("Could not stop battle " + battleUUID + " reason=" + reason, throwable);
      return false;
    }
  }

  public static boolean stopPlayerBattleSafely(ServerPlayerEntity player, String reason) {
    if (player == null) return false;
    var battle = BattleRegistry.getBattleByParticipatingPlayer(player);
    if (battle == null) return false;
    try {
      if (config.isDebug()) {
        LOGGER.info("Stopping player battle player=" + player.getUuid() + " reason=" + reason + " thread=" + Thread.currentThread().getName());
      }
      battle.stop();
      return true;
    } catch (Throwable throwable) {
      LOGGER.error("Could not stop player battle player=" + player.getUuid() + " reason=" + reason, throwable);
      return false;
    }
  }

  public static void executeOnServerThread(String taskName, Runnable runnable) {
    var currentServer = server;
    if (currentServer == null) return;
    currentServer.execute(() -> {
      try {
        runnable.run();
      } catch (Exception e) {
        LOGGER.error("[" + taskName + "] Error: " + e.getMessage(), e);
      }
    });
  }

  public static void load() {
    files();
    DataBaseFactory.init();
  }


  private static void files() {
    config.init();
    language.init();
    categorys.init();
    raidConfigs.init();
    rewardsManager.init();
  }


  private static void events() {
    files();

    CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> {
      CommandTree.register(dispatcher, registry);
    });

    LifecycleEvent.SERVER_STARTED.register(server -> {
      CobbleRaids.server = server;
      load();
    });

    LifecycleEvent.SERVER_STOPPING.register(server -> {
      raidManager.stopAllRaids();
      captureSessionManager.finishAllSessions();
    });

    LifecycleEvent.SERVER_LEVEL_LOAD.register(level -> server = level.getServer());

    PlayerEvent.PLAYER_JOIN.register((player) -> CompletableFuture.runAsync(() -> {
        DataBaseFactory.INSTANCE.findUserByPlayer(player);
        RaidBall.removeRaidBalls(player);
      }, COBBLE_RAID_EXECUTOR)
      .exceptionally(e -> {
        e.printStackTrace();
        return null;
      }));

    PlayerEvent.PLAYER_QUIT.register((player) -> {
      captureSessionManager.finishSessionPlayer(player.getUuid());
      raidManager.stopRaidPlayer(player);
      CompletableFuture.runAsync(() -> {
          var userinfo = DataBaseFactory.INSTANCE.findUserByPlayer(player);
          DataBaseFactory.INSTANCE.saveOrUpdateUserInfo(userinfo);
          DataBaseClient.cacheUser.invalidate(player.getUuid());
        }, COBBLE_RAID_EXECUTOR)
        .exceptionally(e -> {
          e.printStackTrace();
          return null;
        });
    });

    RaidEvents.register();
    PokemonCatchRateEvent.register();
    PokeBallCaptureCompletedEvent.register();
    PokeSentEvent.register();
    BattleStartedPreEvent.register();
    BattleFaintedEvent.register();
    BattleFledEvent.register();
    BattleVictoryEvent.register();
    PokemonCapturedEvent.register();
  }
}
