package com.kingpixel.cobbleraids.models;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.Model.messages.HiperMessage;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import kotlin.Unit;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 13/09/2025 21:37
 */
@Data
public class CaptureSessionData {
  public static final String CAPTURE_NBT_KEY = "capture_session";
  public static final Set<String> REMOVE_PERSISTENT_DATA = Set.of(
    CAPTURE_NBT_KEY,
    Raid.RAID_CATEGORY_NBT_KEY,
    Raid.RAID_NBT_KEY
  );
  private boolean started;
  private boolean finished;
  private UUID battleUUID;
  private RaidData raidData;
  private PokemonEntity pokemonEntity;
  private ServerPlayerEntity player;
  private UUID playerUUID;
  private long startTime;
  private long endTime;

  public CaptureSessionData(Raid raid, ServerPlayerEntity player) {
    this.started = false;
    this.finished = false;
    this.battleUUID = UUID.randomUUID();
    this.pokemonEntity = null;
    this.player = player;
    this.playerUUID = player.getUuid();
    this.raidData = raid.getRaidData();
    this.startTime = System.currentTimeMillis() + raid.getCategoryRaid().getTimeBeforeStart().toMillis();
    this.endTime = raid.getCategoryRaid().getDurationCapture().toMillis() + startTime;
  }

  public synchronized void startSession() {
    CobbleRaids.server.executeSync(() -> {
      HiperMessage message = CobbleRaids.language.getMessageStartCapture();
      message.sendMessage(player, PokemonUtils.replace(message.getRawMessage(), raidData.getCapturePokemonInstance()), CobbleRaids.language.getPrefix(), false);
      CobbleRaids.captureSessionManager.removeSession(battleUUID);
      Pokemon pokemon = PokemonProperties.Companion.parse(raidData.getCapturePokemon()).create();
      markCapturePokemon(pokemon);
      pokemonEntity = pokemon.sendOut(
        (ServerWorld) player.getWorld(),
        player.getPos(),
        null,
        entity -> {
          markCapturePokemon(entity.getPokemon());
          entity.getPokemon().setNickname(player.getName().copy());
          entity.setCustomName(player.getName().copy());
          return Unit.INSTANCE;
        }
      );
      if (pokemonEntity == null) {
        var msg = AdventureTranslator.toNative(
          "&c[&4!&c] &cError spawning the raid Pokemon.",
          CobbleRaids.language.getPrefix()
        );
        player.sendMessage(msg, false);
        cleanupFailedStart();
        return;
      }

      var party = Cobblemon.INSTANCE.getStorage().getParty(player);
      Pokemon leader = null;
      for (Pokemon p : party) {
        if (p != null && !p.isFainted() && !raidData.isBannedPokemon(p)) {
          leader = p;
          break;
        }
      }
      if (leader == null) {
        player.sendMessage(Text.literal("You have no usable Pokemon for the capture battle."), false);
        cleanupFailedStart();
        return;
      }

      var startBattle = BattleBuilder.INSTANCE.pve(
        player,
        pokemonEntity,
        leader.getUuid(),
        BattleFormat.Companion.getGEN_9_SINGLES(),
        false,
        true,
        999,
        party
      );

      startBattle.ifSuccessful(pokemonBattle -> {
        this.battleUUID = pokemonBattle.getBattleId();
        CobbleRaids.captureSessionManager.getActiveSessions().put(this.battleUUID, this);
        CobbleRaids.captureSessionManager.getPlayerSessions().put(player.getUuid(), this);
        this.started = true;
        return Unit.INSTANCE;
      });
      startBattle.ifErrored(erroredBattleStart -> {
        var msg = AdventureTranslator.toNative(
          "&c[&4!&c] &cError starting the session.",
          CobbleRaids.language.getPrefix()
        );
        player.sendMessage(msg, false);
        cleanupFailedStart();
        return Unit.INSTANCE;
      });
    });
  }

  public void checkTimeout() {
    long currentTime = System.currentTimeMillis();
    if (startTime > currentTime) {
      var cooldown = PlayerUtils.getCooldown(startTime);
      Text msg = AdventureTranslator.toNative(
        CobbleRaids.language.getActionBarCaptureStartingIn().replace("%time%", cooldown),
        CobbleRaids.language.getPrefix()
      );
      player.sendMessage(msg, true);
    } else if (!started) {
      boolean isInBattle = BattleRegistry.getBattleByParticipatingPlayer(player) != null;
      if (isInBattle) {
        startTime += TimeUnit.SECONDS.toMillis(15);
      } else {
        startSession();
      }
    } else if (currentTime >= endTime) {
      finishSession();
    } else {
      var cooldown = PlayerUtils.getCooldown(endTime);
      Text msg = AdventureTranslator.toNative(
        CobbleRaids.language.getActionBarCaptureTimeLeft().replace("%time%", cooldown),
        CobbleRaids.language.getPrefix()
      );
      player.sendMessage(msg, true);
    }
  }

  public synchronized void finishSession() {
    if (finished) return;
    finished = true;
    try {
      HiperMessage message = CobbleRaids.language.getMessageEndCapture();
      message.sendMessage(player, PokemonUtils.replace(message.getRawMessage(), raidData.getCapturePokemonInstance()), CobbleRaids.language.getPrefix(), false);
      CobbleRaids.server.execute(() -> {
        var battle = BattleRegistry.getBattle(battleUUID);
        if (battle != null) battle.stop();
        if (pokemonEntity != null) pokemonEntity.discard();
        RaidBall.removeRaidBalls(player);
      });
      CobbleRaids.captureSessionManager.clearPendingRaidBallCatchRate(playerUUID);
      CobbleRaids.captureSessionManager.removeSession(battleUUID);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void addTime() {
    var time = TimeUnit.SECONDS.toMillis(10);
    if (endTime - System.currentTimeMillis() > time) return;
    this.endTime += time;
  }

  public void setTime() {
    this.endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
  }

  private void markCapturePokemon(Pokemon pokemon) {
    if (pokemon == null) return;
    var persistentData = pokemon.getPersistentData();
    persistentData.putString(Raid.RAID_CATEGORY_NBT_KEY, raidData.getCategoryRaid().getId());
    persistentData.putString(CAPTURE_NBT_KEY, playerUUID.toString());
  }

  private void cleanupFailedStart() {
    started = false;
    CobbleRaids.captureSessionManager.removeSession(battleUUID);
    CobbleRaids.captureSessionManager.clearPendingRaidBallCatchRate(playerUUID);
    if (pokemonEntity != null) {
      pokemonEntity.discard();
      pokemonEntity = null;
    }
    RaidBall.removeRaidBalls(player);
  }
}
