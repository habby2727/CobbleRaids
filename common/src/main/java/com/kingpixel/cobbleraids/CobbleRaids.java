package com.kingpixel.cobbleraids;

import com.cobblemon.mod.common.Cobblemon;
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
import com.kingpixel.cobbleutils.CobbleUtils;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.TypedActionResult;

import java.util.concurrent.*;

public class CobbleRaids {
  public static final String MOD_ID = "cobbleraids";
  public static final String MOD_NAME = "CobbleRaids";
  public static final String PATH = "/config/" + MOD_ID;
  public static final String PATH_LANG = PATH + "/lang/";
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

  public static void init() {
    events();
    files();
    tasks();
  }

  private static void tasks() {
    // Task to teleport out players that are fighting but are no longer in the raid area.
    COBBLE_RAIDS_SCHEDULER.scheduleWithFixedDelay(() -> {
        try {
          if (server == null) return;
          if (raidManager == null) return;
          var fights = raidManager.getFightingByBattleUUID();
          if (fights == null || fights.isEmpty()) return;
          fights.forEach((key, value) -> {
            var player = value.getPlayer();
            var raid = value.getRaid();
            if (player == null || raid == null) return;
            raid.teleportPlayerOut(player);
          });
        } catch (Exception e) {
          e.printStackTrace();
        }
      }, 0, 50, TimeUnit.MILLISECONDS
    );

    // Task to update the boss bar of all active raids.
    COBBLE_RAIDS_SCHEDULER.scheduleWithFixedDelay(() -> {
      try {
        if (server == null) return;
        if (raidManager == null) return;
        var activeRaids = raidManager.getActiveRaids();
        if (activeRaids == null || activeRaids.isEmpty()) return;
        activeRaids.forEach((key, value) -> {
          value.getCategoryRaid().manageBossBar(value);
        });
      } catch (Exception e) {
        e.printStackTrace();
        CobbleUtils.LOGGER.error("[BossBarTask] Error during boss bar management: " + e.getMessage());
      }
    }, 0, 1, TimeUnit.SECONDS);

    // Task to finish raids that finish by time.
    COBBLE_RAIDS_SCHEDULER.scheduleWithFixedDelay(() -> {
      try {
        if (server == null) return;
        if (raidManager == null) return;
        var activeRaids = raidManager.getActiveRaids();
        if (activeRaids == null || activeRaids.isEmpty()) return;
        activeRaids.forEach((key, value) -> {
          value.sendActionBarTimeLeft();
          if (value.isFinishByTime()) {
            value.finishRaid();
          }
        });
      } catch (Exception e) {
        e.printStackTrace();
      }
    }, 0, 1, TimeUnit.SECONDS);
    // Task Capture session timeout
    COBBLE_RAIDS_SCHEDULER.scheduleWithFixedDelay(() -> {
      try {
        if (server == null) return;
        if (captureSessionManager == null) return;
        var sessions = captureSessionManager.getActiveSessions();
        if (sessions == null || sessions.isEmpty()) return;
        sessions.forEach((key, value) -> value.checkTimeout());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }, 0, 1, TimeUnit.SECONDS);

    // Task to Init a random raid if no raid is active.
    COBBLE_RAIDS_SCHEDULER.scheduleWithFixedDelay(() -> {
      try {
        if (server == null) return;
        if (raidManager == null) return;
        if (!raidManager.isRandomRaidOn()) {
          raidManager.initRandomRaid();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }, 0, 1, TimeUnit.SECONDS);

    // Task to check if the player continue in a battle if the player disconnect and reconnect.
    COBBLE_RAIDS_SCHEDULER.scheduleWithFixedDelay(() -> {
      if (server == null) return;
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
        var battle = Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player);
        if (battle == null) fightData.stop(true);
      }
    }, 0, 10, TimeUnit.SECONDS);
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
      var fightData = raidManager.getFightingPlayer(player.getUuid());
      if (fightData != null) fightData.stop(true);
      CompletableFuture.runAsync(() -> {
          var userinfo = DataBaseFactory.INSTANCE.findUserByPlayer(player);
          DataBaseFactory.INSTANCE.saveOrUpdateUserInfo(userinfo);
          DataBaseClient.cacheUser.invalidate(player.getUuid());
        }, COBBLE_RAID_EXECUTOR)
        .exceptionally(e -> {
          e.printStackTrace();
          return null;
        });
      raidManager.stopRaidPlayer(player);
      captureSessionManager.finishSessionPlayer(player.getUuid());
    });

    UseItemCallback.EVENT.register((playerEntity, world, hand) -> {
      ItemStack stack = playerEntity.getStackInHand(hand);
      if (!stack.isEmpty() && RaidBall.isRaidBall(stack)) {
        var captureSession = captureSessionManager.getSessionByPlayer(playerEntity.getUuid());
        return captureSession == null ?
          TypedActionResult.fail(stack) :
          TypedActionResult.success(stack);
      }
      return TypedActionResult.success(stack);
    });

    RaidEvents.register();
    PokeBallCaptureCompletedEvent.register();
    PokeSentEvent.register();
    BattleStartedPreEvent.register();
    BattleFaintedEvent.register();
    BattleFledEvent.register();
    BattleVictoryEvent.register();
    PokemonCapturedEvent.register();
  }
}
