package com.kingpixel.cobbleraids.models;

import ca.landonjw.gooeylibs2.api.UIManager;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.database.DataBaseFactory;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import kotlin.Unit;
import lombok.Data;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 13/09/2025 2:28
 */
@Data
public class Raid {
  public static final String RAID_NBT_KEY = "CobbleRaids_RaidUUID";
  private boolean finish;
  private long startTime;
  private UUID raidUUID;
  private PokemonEntity raidEntity;
  private RaidData raidData;
  private CategoryRaid categoryRaid;
  private int health;
  private int maxHealth;
  private Map<UUID, Integer> damageMap;

  public Raid(RaidData raidData) {
    this.finish = false;
    this.startTime = System.currentTimeMillis();
    this.raidUUID = UUID.randomUUID();
    this.raidData = raidData;
    this.categoryRaid = raidData.getCategoryRaid();
    this.health = categoryRaid.getHealth();
    this.maxHealth = categoryRaid.getHealth();
    this.damageMap = new HashMap<>();
    this.raidEntity = generateRaidEntity(false);
  }

  private String actualPhase = "";

  private void refreshRaidEntity() {
    var phase = raidData.getActualPhase(this);
    if (phase.equals(actualPhase)) return;
    actualPhase = phase;
    Pokemon pokemon = PokemonProperties.Companion.parse(phase).create();
    Pokemon pokemonRaid = raidEntity.getPokemon();
    pokemonRaid.setSpecies(pokemon.getSpecies());
    pokemonRaid.setForm(pokemon.getForm());
    pokemonRaid.setForcedAspects(pokemon.getForcedAspects());
    pokemonRaid.setGender(pokemon.getGender());
    pokemonRaid.setShiny(pokemon.getShiny());
    CobbleRaids.server.execute(() -> raidEntity.getPokemon().updateAspects());
  }

  private void startBattle(ServerPlayerEntity player) {
    if (finish || health <= 0) return;
    UserInfo userInfo = DataBaseFactory.INSTANCE.findUserByPlayer(player);
    if (categoryRaid.isNeedTicket() && !damageMap.containsKey(player.getUuid())) {
      if (!userInfo.hasTicket(categoryRaid)) {
        PlayerUtils.sendMessage(
          player,
          "§c[§6CobbleRaids§c] §cYou don't have a ticket to join this raid!§r",
          CobbleRaids.language.getPrefix(),
          TypeMessage.CHAT
        );
        return;
      }
    }
    PokemonEntity raidEntity = generateRaidEntity(true);
    raidEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, Integer.MAX_VALUE, 255));
    if (!CobbleRaids.config.isDebug()) {
      raidEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, Integer.MAX_VALUE, 255));
    }

    var party = Cobblemon.INSTANCE.getStorage().getParty(player);
    Pokemon leader = null;
    for (Pokemon pokemon : party) {
      if (pokemon != null) {
        leader = pokemon;
      }
    }
    if (leader == null) return;

    Pokemon finalLeader = leader;

    var startBattle = BattleBuilder.INSTANCE.pve(
      player,
      raidEntity,
      finalLeader.getUuid(),
      BattleFormat.Companion.getGEN_9_SINGLES(),
      true,
      true,
      Cobblemon.INSTANCE.getConfig().getDefaultFleeDistance(),
      party
    );

    startBattle.ifSuccessful(pokemonBattle -> {
      var actor = (PlayerBattleActor) pokemonBattle.getActor(player);
      if (actor == null) {
        CobbleUtils.LOGGER.fatal("Could not find actor for player " + player.getName().getString() + " in battle " + pokemonBattle.getBattleId());
        return Unit.INSTANCE;
      }
      actor.getPokemonList().removeIf(pokemon -> categoryRaid.isBlackList(pokemon.getEffectedPokemon()));
      if (actor.getPokemonList().isEmpty()) {
        PlayerUtils.sendMessage(
          player,
          "§c[§6CobbleRaids§c] §cYou don't have any valid Pokémon to fight this raid!§r",
          CobbleRaids.language.getPrefix(),
          TypeMessage.CHAT
        );
        CobbleRaids.server.execute(pokemonBattle::stop);
        return Unit.INSTANCE;
      }
      CobbleRaids.raidManager.addFightingPlayer(
        pokemonBattle.getBattleId(),
        new FightData(
          pokemonBattle.getBattleId(),
          this,
          player,
          raidEntity
        )
      );

      PlayerUtils.sendMessage(
        player,
        "§a[§6CobbleRaids§a] §aYou have started a battle against a raid boss!§r",
        CobbleRaids.language.getPrefix(),
        TypeMessage.CHAT
      );
      if (!damageMap.containsKey(player.getUuid())) {
        userInfo.removeTicket(categoryRaid);
        maxHealth += categoryRaid.getHealth();
        health += categoryRaid.getHealth();
        damageMap.put(player.getUuid(), 0);
      }
      teleportPlayerOut(player);
      return Unit.INSTANCE;
    });
    startBattle.ifErrored(erroredBattleStart -> {
      PlayerUtils.sendMessage(
        player,
        "§c[§6CobbleRaids§c] §cCould not start the battle, please try again later.§r",
        CobbleRaids.language.getPrefix(),
        TypeMessage.CHAT
      );
      return Unit.INSTANCE;
    });

  }

  private PokemonEntity generateRaidEntity(boolean fight) {
    var result = raidData.getActualPhase(this);
    var properties = PokemonProperties.Companion.parse(result);
    var coords = categoryRaid.getCoords();
    var pokemon = properties.create();
    if (!fight) {
      pokemon.getPersistentData().putUuid(
        RAID_NBT_KEY,
        raidUUID
      );
    }
    ServerWorld serverWorld = categoryRaid.getWorld();
    int x = coords.x() >> 4;
    int z = coords.z() >> 4;
    if (!serverWorld.isChunkLoaded(x, z)) {
      serverWorld.getChunkManager().getChunk(x, z, ChunkStatus.FULL, false);
    }
    var pokemonEntity = pokemon.sendOut(
      categoryRaid.getWorld(),
      coords.getVec3d(),
      null,
      entity -> {
        categoryRaid.compute(entity, fight);
        return Unit.INSTANCE;
      }
    );
    if (pokemonEntity == null) {
      throw new IllegalStateException("Could not create PokemonEntity for raid");
    }
    return pokemonEntity;

  }

  public void teleportPlayerOut(ServerPlayerEntity player) {
    double radio = categoryRaid.getRadio();
    Vec3d coords = categoryRaid.getCoords().getVec3d();
    Vec3d posPlayer = player.getPos();
    if (!posPlayer.isInRange(coords, radio)) return;
    Vec3d direction = posPlayer.subtract(coords).normalize();
    Vec3d newPos = coords.add(direction.multiply(radio + 5));
    CobbleRaids.server.execute(() -> player.teleport(
      categoryRaid.getWorld(),
      newPos.x,
      posPlayer.getY(),
      newPos.z,
      player.getYaw(),
      player.getPitch()
    ));
  }

  public void updateHealth(FightData fightData, int damage) {
    fightData.stop();
    ServerPlayerEntity player = fightData.getPlayer();
    damageMap.compute(player.getUuid(), (k, damageFromMap) -> (damageFromMap == null ? 0 : damageFromMap) + damage);
    removeDamage(damage);
    if (CobbleRaids.config.isDebug()) {
      PlayerUtils.sendMessage(
        player,
        "§e[§6CobbleRaids§e] §eRaid Boss Health: " + health + "/" + maxHealth + " (-" + damage + ")§r",
        CobbleRaids.language.getPrefix(),
        TypeMessage.CHAT
      );
    }
    if (health > 0) {
      refreshRaidEntity();
      startBattle(player);
    } else finishRaid();
  }

  private synchronized void removeDamage(int damage) {
    health -= damage;
  }

  private synchronized void finishRaid() {
    if (finish) return;
    finish = true;
    if (raidEntity == null) return;
    raidEntity.remove(Entity.RemovalReason.DISCARDED);
    CobbleRaids.raidManager.removeRaid(raidUUID);
    var fights = CobbleRaids.raidManager.getFightingPlayers(raidUUID);
    for (FightData fight : fights) {
      UIManager.closeUI(fight.getPlayer());
      fight.stop();
    }
    CobbleRaids.rewardsManager.giveRewards(categoryRaid, damageMap);
  }

  public void openStartBattleMenu(ServerPlayerEntity player) {
    if (health <= 0 || finish) {
      PlayerUtils.sendMessage(
        player,
        "§c[§6CobbleRaids§c] §cThis raid has already finished.§r",
        CobbleRaids.language.getPrefix(),
        TypeMessage.CHAT
      );
      return;
    }
    if (PlayerUtils.isBattle(player)) return;
    CobbleRaids.language.getStartBattleRaid().open(
      player,
      raidData.getActualPhasePokemonItem(this),
      confirm -> {
        startBattle(player);
        UIManager.closeUI(player);
      },
      cancel -> {
        PlayerUtils.sendMessage(
          player,
          "§c[§6CobbleRaids§c] §cYou have cancelled the raid battle.§r",
          CobbleRaids.language.getPrefix(),
          TypeMessage.CHAT
        );
        UIManager.closeUI(player);
      }
    );
  }
}
