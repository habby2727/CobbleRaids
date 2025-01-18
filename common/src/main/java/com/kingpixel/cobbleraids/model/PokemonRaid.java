package com.kingpixel.cobbleraids.model;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import kotlin.Unit;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 17/01/2025 23:04
 */
@Getter
@Setter
@ToString
public class PokemonRaid {
  private String pokemon;
  private Double chance;
  private Integer life;
  private float size;
  private BlackListRaid banned;

  PokemonRaid() {
    this.pokemon = "pikachu";
    this.chance = 0.1;
    this.life = 10000;
    this.size = 1.0f;
    this.banned = new BlackListRaid();
  }

  public Pokemon genPokemon() {
    return PokemonProperties.Companion.parse(pokemon + " uncatchable=yes").create();
  }

  public static ServerWorld getWorld(Raid raid) {
    ServerWorld serverWorld = null;
    for (ServerWorld world : CobbleRaids.server.getWorlds()) {
      String nameSpace = world.getRegistryKey().getValue().getPath();
      if (CobbleRaids.config.isDebug()) {
        CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "World: " + nameSpace);
      }
      if (nameSpace.equals(raid.getWorld())) {
        serverWorld = world;
        break;
      }
    }
    return serverWorld;
  }

  public PokemonEntity genPokemonEntity(Raid raid) {
    ServerWorld serverWorld = getWorld(raid);

    if (serverWorld == null) {
      CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "World not found: " + raid.getWorld());
      return null;
    }
    Vec3d pos = new Vec3d(raid.getPosRaid().x, raid.getPosRaid().y, raid.getPosRaid().z);
    if (CobbleRaids.config.isDebug()) {
      CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "Pos: " + pos);
    }
    List<Entity> entities = serverWorld.getOtherEntities(null, Box.of(pos, 32, 32, 32));
    for (Entity entity : entities) {
      if (entity instanceof PokemonEntity pokemonEntity) {
        NbtCompound nbt = pokemonEntity.getPokemon().getPersistentData();
        if (nbt.getBoolean(CobbleRaids.TAG_RAID) || nbt.getBoolean(CobbleRaids.TAG_FAKERAID)) {
          entity.remove(Entity.RemovalReason.DISCARDED);
        }
      }
    }
    Pokemon raidPokemon = PokemonProperties.Companion.parse(pokemon + " uncatchable=yes").create();
    raidPokemon.getPersistentData().putBoolean(CobbleRaids.TAG_RAID, true);
    raidPokemon.setScaleModifier(size);
    PokemonEntity pokemonEntity = raidPokemon
      .sendOut(serverWorld, pos, null,
        pokemonEntity1 -> Unit.INSTANCE);
    if (pokemonEntity == null) {
      CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "Pokemon not found: " + pokemonEntity);
      return null;
    }
    pokemonEntity.setCustomName(AdventureTranslator.toNative(raid.getName()));
    pokemonEntity.setMovementSpeed(0);
    pokemonEntity.setNoGravity(true);
    pokemonEntity.setAiDisabled(true);
    pokemonEntity.setGlowing(true);
    pokemonEntity.teleport(raid.getPosRaid().x, raid.getPosRaid().y, raid.getPosRaid().z, false);
    return pokemonEntity;
  }

  /**
   * Check if the player is permited to join the raid
   *
   * @param player the player to check
   *
   * @return true if the player is permited to join the raid
   */
  public boolean isPermitted(ServerPlayerEntity player) {
    for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getParty(player)) {
      if (CobbleRaids.config.getBanned().isBanned(player, pokemon)) return false;
      if (CobbleRaids.battleManager.getPokemonRaid().getBanned().isBanned(player, pokemon)) return false;
    }
    return true;
  }
}
