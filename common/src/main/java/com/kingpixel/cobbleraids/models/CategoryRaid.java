package com.kingpixel.cobbleraids.models;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.api.PermissionApi;
import lombok.Data;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Carlos Varas Alonso - 13/09/2025 2:35
 */
@Data
public class CategoryRaid {
  private static Map<String, ServerWorld> worldMap = new HashMap<>();
  private String id;
  private String name;
  private String permission;
  private boolean needTicket;
  private String bossBarName;
  private BossBar.Style bossBarStyle;
  private BossBar.Color bossBarColor;
  private DurationValue durationRaid;
  private DurationValue timeBeforeStart;
  private DurationValue durationCapture;
  private int health;
  private int overLevel;
  private int minLevel;
  private int size;
  private double radio;
  private AdvancedBlacklist blacklist;
  private String world;
  private Coords coords;
  // TODO: Glowing option

  public CategoryRaid() {
    this.id = "default";
    this.name = "Default Raid";
    this.needTicket = false;
    this.permission = "";
    this.bossBarName = "Raid ⭐ | %pokemon% | %health%/%maxhealth% HP";
    this.bossBarStyle = BossBar.Style.PROGRESS;
    this.bossBarColor = BossBar.Color.RED;
    this.durationRaid = DurationValue.parse("30m");
    this.timeBeforeStart = DurationValue.parse("30s");
    this.durationCapture = DurationValue.parse("5m");
    this.health = 1000;
    this.overLevel = 100;
    this.minLevel = 1;
    this.size = 2;
    this.radio = 5;
    this.blacklist = new AdvancedBlacklist();
    this.world = "minecraft:overworld";
    this.coords = new Coords(0, 70, 0);
  }

  public CategoryRaid(String id) {
    super();
    this.id = id;
  }

  public int getHealth() {
    if (health <= 0) return 100;
    return health;
  }

  public boolean havePermission(ServerPlayerEntity player) {
    return PermissionApi.hasPermission(player, permission, 2);
  }

  public void manageBossBar(Raid value) {
    var bossBar = value.getBossBar();
    value.refreshBossBar();
    var players = value.getPlayersInWorld();
    if (bossBar == null || players == null || players.isEmpty()) {
      return;
    }
    for (ServerPlayerEntity player : players) {
      if (player.isInRange(
        value.getRaidEntity(),
        64
      )) {
        bossBar.addPlayer(player);
      } else {
        bossBar.removePlayer(player);
      }
    }
  }

  public ServerWorld getWorld() {
    var serverWorld = worldMap.get(this.world);
    if (serverWorld != null) return serverWorld;
    for (ServerWorld world1 : CobbleRaids.server.getWorlds()) {
      if (world1.getRegistryKey().getValue().toString().equals(this.world)) {
        worldMap.put(this.world, world1);
        return world1;
      }
    }
    return CobbleRaids.server.getOverworld();
  }

  public boolean haveMinLevel(ServerPlayerEntity player) {
    var party = Cobblemon.INSTANCE.getStorage().getParty(player);
    var list = party.toGappyList();
    for (Pokemon pokemon : list) {
      if (pokemon.getLevel() < minLevel) return false;
    }
    return true;
  }

  public void compute(PokemonEntity pokemonEntity, boolean fight) {
    var pokemon = pokemonEntity.getPokemon();
    int oldLevel = CobbleRaids.oldLevelCap;
    if (overLevel > oldLevel) Cobblemon.INSTANCE.getConfig().setMaxPokemonLevel(overLevel);
    pokemon.setLevel(overLevel);
    if (overLevel > oldLevel) Cobblemon.INSTANCE.getConfig().setMaxPokemonLevel(oldLevel);
    pokemonEntity.setNoGravity(true);
    pokemonEntity.speed = 0;
    pokemonEntity.setHealth(pokemonEntity.getMaxHealth());
    if (!fight) pokemonEntity.setPersistent();
    pokemonEntity.setAiDisabled(true);
    pokemonEntity.setMovementSpeed(0);
    if (fight) {
      pokemon.setScaleModifier(0.1f);
    } else {
      pokemon.setScaleModifier(size);
    }
  }

  public boolean isBlackList(Pokemon pokemon) {
    if (blacklist.isBanned(pokemon)) return true;
    return CobbleRaids.config.getBlacklist().isBanned(pokemon);
  }


  public record Coords(int x, int y, int z) {
    private static Vec3d vec3d;

    public Vec3d getVec3d() {
      if (vec3d != null) return vec3d;
      vec3d = new Vec3d(x, y, z);
      return vec3d;
    }
  }
}
