package com.kingpixel.cobbleraids;

import ca.landonjw.gooeylibs2.api.tasks.Task;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.kingpixel.cobbleraids.command.CommandTree;
import com.kingpixel.cobbleraids.config.Config;
import com.kingpixel.cobbleraids.config.Lang;
import com.kingpixel.cobbleraids.config.RaidsConfig;
import com.kingpixel.cobbleraids.events.BattleEvents;
import com.kingpixel.cobbleraids.events.CaptureEvents;
import com.kingpixel.cobbleraids.managers.BattleManager;
import com.kingpixel.cobbleutils.CobbleUtils;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CobbleRaids {
  public static final String MOD_ID = "cobbleraids";
  public static final String MOD_NAME = "CobbleRaids";
  public static final String PATH = "/config/" + MOD_ID;
  public static final String PATH_LANG = PATH + "/lang/";
  public static final String PATH_RAIDS = PATH + "/raids/";
  private static final String PATH_REWARDS = PATH + "/rewards";
  public static final String PATH_RAID_DAMAGE_REWARDS = PATH_REWARDS + "/damageRewards/";
  public static final String PATH_RAID_GLOBAL_REWARDS = PATH_REWARDS + "/globalRewards/";
  public static final String PATH_RAID_KILL_REWARDS = PATH_REWARDS + "/killRewards/";
  public static final String TAG_RAID = "raid";
  public static final String TAG_FAKERAID = "fakeRaid";
  public static final String TAG_RAID_ID = "raidId";
  public static final String TAG_RAID_ACTIVE = "raidActive";
  public static final String TAG_RAID_CAPTURE = "raidCapture";
  public static MinecraftServer server;
  public static Config config = new Config();
  public static Lang language = new Lang();
  public static RaidsConfig raidsConfig = new RaidsConfig();
  public static Date startDate;
  public static Task removeOldRaidsTask;

  public static void init() {
    events();
  }

  public static void load() {
    files();
    tasks();
    startDate = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(config.getCooldown()));
  }

  private static void tasks() {
    if (removeOldRaidsTask != null) {
      removeOldRaidsTask.setExpired();
    }
    removeOldRaidsTask = Task.builder()
      .execute(() -> {
          for (ServerWorld world : server.getWorlds()) {
            if (world == null) continue;
            var entities = world.getEntitiesByType(TypeFilter.instanceOf(PokemonEntity.class), e -> {
              if (e == null) return false;
              NbtCompound nbt = e.getPokemon().getPersistentData();
              if (nbt.getBoolean(TAG_RAID_CAPTURE)) return false;
              if (nbt.getBoolean(TAG_RAID) || nbt.getBoolean(TAG_FAKERAID)) {
                if (!nbt.contains(TAG_RAID_ACTIVE)) return false;
                UUID raidUUID = nbt.getUuid(TAG_RAID_ACTIVE);
                if (raidUUID == null) return false;
                return BattleManager.getActiveRaid(raidUUID) == null;
              }
              return false;
            });
            if (entities == null || entities.isEmpty()) continue;
            for (PokemonEntity pokemon : entities) {
              pokemon.remove(Entity.RemovalReason.DISCARDED);
            }
          }
        }
      ).interval(20 * 60)
      .infinite()
      .build();


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

    LifecycleEvent.SERVER_LEVEL_LOAD.register(level -> {
      server = level.getServer();
      for (ServerWorld world : server.getWorlds()) {
        var pokemons = world.getEntitiesByType(TypeFilter.instanceOf(PokemonEntity.class), e -> {
          NbtCompound nbt = e.getPokemon().getPersistentData();
          return nbt.getBoolean(TAG_RAID) || nbt.getBoolean(TAG_FAKERAID);
        });
        for (PokemonEntity pokemon : pokemons) {
          if (config.isDebug()) {
            CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "Removing raid pokemon: " + pokemon.getPokemon().showdownId());
          }
          pokemon.remove(Entity.RemovalReason.DISCARDED);
        }
      }
    });

    BattleEvents.register();
    CaptureEvents.register();
  }
}
