package com.kingpixel.cobbleraids;

import com.cobblemon.mod.common.Cobblemon;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kingpixel.cobbleraids.command.CommandTree;
import com.kingpixel.cobbleraids.config.*;
import com.kingpixel.cobbleraids.database.DataBaseClient;
import com.kingpixel.cobbleraids.database.DataBaseFactory;
import com.kingpixel.cobbleraids.events.BattleEvents;
import com.kingpixel.cobbleraids.manager.RaidManager;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.server.MinecraftServer;

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
  public static final Executor COBBLE_RAID_EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
    .setNameFormat("Cobble-Raids-executor-%d")
    .setDaemon(true)
    .build());
  private static final ScheduledExecutorService COBBLE_RAIDS_SCHEDULER = Executors.newSingleThreadScheduledExecutor(
    new ThreadFactoryBuilder()
      .setNameFormat("Cobble-Raids-scheduler-%d")
      .setDaemon(true)
      .build()
  );

  public static void init() {
    events();
    tasks();
  }

  private static void tasks() {
    COBBLE_RAIDS_SCHEDULER.scheduleWithFixedDelay(() -> {
        if (server == null) return;
        if (raidManager == null) return;
        var fights = raidManager.getFightingPlayers();
        if (fights == null || fights.isEmpty()) return;
        fights.forEach((key, value) -> {
          var player = value.getPlayer();
          var raid = value.getRaid();
          if (player == null || raid == null) return;
          raid.teleportPlayerOut(player);
        });
      }, 0, 50, TimeUnit.MILLISECONDS
    );
  }

  public static void load() {
    files();
    DataBaseFactory.init();
  }


  private static void files() {
    config.init();
    language.init();
    raidConfigs.init();
    categorys.init();
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
      CobbleRaids.raidManager.stopAllRaids();
    });

    LifecycleEvent.SERVER_LEVEL_LOAD.register(level -> server = level.getServer());

    PlayerEvent.PLAYER_JOIN.register((player) -> {
      CompletableFuture.runAsync(() -> DataBaseFactory.INSTANCE.findUserByPlayer(player), COBBLE_RAID_EXECUTOR)
        .exceptionally(e -> {
          e.printStackTrace();
          return null;
        });
    });

    PlayerEvent.PLAYER_QUIT.register((player) -> {
      CompletableFuture.runAsync(() -> {
          var userinfo = DataBaseFactory.INSTANCE.findUserByPlayer(player);
          DataBaseFactory.INSTANCE.saveOrUpdateUserInfo(userinfo);
          DataBaseClient.cache.remove(player.getUuid());
        }, COBBLE_RAID_EXECUTOR)
        .exceptionally(e -> {
          e.printStackTrace();
          return null;
        });
    });

    BattleEvents.register();
  }
}
