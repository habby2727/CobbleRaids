package com.kingpixel.cobbleraids;

import ca.landonjw.gooeylibs2.api.tasks.Task;
import com.kingpixel.cobbleraids.command.CommandTree;
import com.kingpixel.cobbleraids.config.Config;
import com.kingpixel.cobbleraids.config.Lang;
import com.kingpixel.cobbleraids.config.RaidsConfig;
import com.kingpixel.cobbleraids.events.BattleEvents;
import com.kingpixel.cobbleraids.managers.BattleManager;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.server.MinecraftServer;

import java.util.Date;

public class CobbleRaids {
  public static final String MOD_ID = "cobbleraids";
  public static final String MOD_NAME = "CobbleRaids";
  public static final String PATH = "/config/" + MOD_ID;
  public static final String PATH_LANG = PATH + "/lang/";
  public static final String PATH_RAIDS = PATH + "/tags/";
  public static final String PATH_RAID_DAMAGE_REWARDS = PATH + "/damageRewards/";
  public static final String PATH_RAID_PARTICIPATION_REWARDS = PATH + "/participationRewards";
  public static final String PATH_RAID_GLOBAL_REWARDS = PATH + "/serverRewards";
  public static MinecraftServer server;
  public static Config config = new Config();
  public static Lang language = new Lang();
  public static RaidsConfig raidsConfig = new RaidsConfig();
  public static BattleManager battleManager;
  private static Task startRaid;
  private static Task endRaid;
  public static Date startDate;
  public static Date endDate;


  public static void init() {
    events();
  }

  public static void load() {
    files();
    tasks();
  }

  private static void tasks() {
    if (startRaid != null) {
      startRaid.setExpired();
    }
    startRaid = Task.builder()
      .execute(() -> {
        BattleManager.startRaid();
      })
      .delay((long) config.getStartShowBar() * 20 * 60)
      .infinite()
      .build();
    if (endRaid != null) {
      endRaid.setExpired();
    }
  }


  private static void files() {
    config.init();
    language.init();
    raidsConfig.init();
  }


  private static void events() {
    files();


    CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> {
      CommandTree.register(dispatcher, registry);
    });

    LifecycleEvent.SERVER_STARTED.register(server -> {
      load();
    });


    LifecycleEvent.SERVER_LEVEL_LOAD.register(level -> server = level.getServer());


    BattleEvents.register();
  }
}
