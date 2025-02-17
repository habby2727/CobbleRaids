package com.kingpixel.cobbleraids.model;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleStartError;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import kotlin.Unit;
import lombok.Data;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 06/02/2025 4:02
 */
@Data
public class CaptureSession {
  public static List<CaptureSession> activeCaptures = new CopyOnWriteArrayList<>();
  private static List<ServerPlayerEntity> players = new CopyOnWriteArrayList<>();
  private boolean started;
  private UUID pokemonUUID;
  private PokemonRaid pokemonRaid;
  private ServerPlayerEntity player;
  private PokemonEntity pokemonEntity;
  private int initInSeconds;
  private int finishInSeconds;

  public CaptureSession(PokemonRaid pokemon, ServerPlayerEntity player) {
    this.pokemonRaid = pokemon;
    this.player = player;
    this.initInSeconds = CobbleRaids.config.getSecondsToStartCapture();
    this.finishInSeconds = CobbleRaids.config.getSecondsToFinishCapture();
  }

  public static void removeUuid(UUID uuid) {
    activeCaptures.removeIf(capture -> {
      boolean equals = capture.getPokemonUUID().equals(uuid);
      if (equals) {
        players.remove(capture.getPlayer());
      }
      return equals;
    });
  }

  public static void capturing(UUID uuid) {
    activeCaptures.stream().filter(capture -> capture.getPokemonUUID().equals(uuid)).findFirst().ifPresent(capture -> {
      if (capture.getFinishInSeconds() < 10) {
        capture.setFinishInSeconds(10);
      }
    });
  }

  public void initCaptureFight() {
    var battle = Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player);
    if (players.contains(player) || battle != null) {
      initInSeconds = CobbleRaids.config.getSecondsToStartCapture();
      finishInSeconds = CobbleRaids.config.getSecondsToFinishCapture();
      return;
    }


    Pokemon pokemon = pokemonRaid.obtainPokemonCapture();
    pokemon.getPersistentData().putBoolean(CobbleRaids.TAG_RAID_CAPTURE, true);
    ServerWorld world = (ServerWorld) player.getEntityWorld();

    if (world == null) {
      CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "World is null");
      return;
    }


    Vec3d pos = new Vec3d(player.getPos().x + 1, player.getPos().y, player.getPos().z + 1);

    pokemonEntity = pokemon.sendOut(world, pos, null, entity -> {
      entity.setAiDisabled(true);
      return Unit.INSTANCE;
    });

    if (pokemonEntity == null) {
      CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "Pokemon entity is null");
      return;
    }

    var party = Cobblemon.INSTANCE.getStorage().getParty(player);
    UUID pokemonUUID = null;
    for (Pokemon pokemonParty : party) {
      if (!pokemonParty.isFainted()) {
        pokemonUUID = pokemonParty.getUuid();
        break;
      }
    }

    if (pokemonUUID == null) {
      CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "Pokemon UUID is null");
      return;
    }


    pokemonEntity.heal(pokemonEntity.getMaxHealth());
    pokemonEntity.getPokemon().heal();

    var battleStartResult = BattleBuilder.INSTANCE.pve(
      player,
      pokemonEntity,
      pokemonUUID,
      BattleFormat.Companion.getGEN_9_SINGLES(),
      false,
      true,
      Cobblemon.config.getDefaultFleeDistance(),
      party
    );
    battleStartResult.ifErrored(e -> {
      CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "Error: " + e);
      for (BattleStartError error : e.getErrors()) {
        player.sendMessage(error.getMessageFor(player));
      }
      var pokemonBattle = Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player);
      if (pokemonBattle != null) {
        pokemonBattle.setEnded(true);
        pokemonBattle.end();
      }
      pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
      return Unit.INSTANCE;
    });
    battleStartResult.ifSuccessful(battle1 -> {
      started = true;
      PlayerUtils.sendMessage(
        player,
        "You are battling in capture fight!",
        CobbleRaids.config.getPrefix(),
        TypeMessage.CHAT
      );
      return Unit.INSTANCE;
    });
    this.pokemonUUID = pokemonEntity.getPokemon().getUuid();
  }

  public void finishCaptureFight() {
    if (pokemonEntity == null) return;
    var battle = Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player);
    if (battle != null) {
      if (CobbleRaids.config.isDebug()) {
        CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "Battle is not null finishing capture fight");
      }
      battle.setEnded(true);
      battle.end();
    }
    pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
  }

  public void sendInitMessage() {
    PlayerUtils.sendMessage(
      player,
      CobbleRaids.language.getMessageStartingCapture()
        .replace("%cooldown%", PlayerUtils.getCooldown(new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(initInSeconds)))),
      CobbleRaids.config.getPrefix(),
      TypeMessage.ACTIONBAR
    );
    initInSeconds--;
  }

  public void sendFinishMessage() {
    PlayerUtils.sendMessage(
      player,
      CobbleRaids.language.getMessageFinishingCapture()
        .replace("%cooldown%", PlayerUtils.getCooldown(new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(finishInSeconds)))),
      CobbleRaids.config.getPrefix(),
      TypeMessage.ACTIONBAR
    );
    finishInSeconds--;
  }
}
