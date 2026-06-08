package com.kingpixel.cobbleraids.models;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleStartError;
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
  private static final float CAPTURE_FLEE_DISTANCE = 1_000_000F;
  public static final Set<String> REMOVE_PERSISTENT_DATA = Set.of(
    CAPTURE_NBT_KEY,
    Raid.RAID_CATEGORY_NBT_KEY,
    Raid.RAID_NBT_KEY
  );
  private boolean starting;
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
    this.starting = false;
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
    if (starting || started || finished) return;
    starting = true;
    CobbleRaids.executeOnServerThread("CaptureSessionStart", () -> {
      try {
      ServerPlayerEntity currentPlayer = getOnlinePlayer();
      if (currentPlayer == null) {
        cancelPendingSession("player-offline-before-start", false);
        return;
      }
      if (!isPlayerInRaidArea(currentPlayer)) {
        sendCaptureCancelledMessage(currentPlayer);
        cancelPendingSession("left-raid-area-before-start", false);
        return;
      }
      player = currentPlayer;
      HiperMessage message = CobbleRaids.language.getMessageStartCapture();
      message.sendMessage(player, PokemonUtils.replace(message.getRawMessage(), raidData.getCapturePokemonInstance()), CobbleRaids.language.getPrefix(), false);
      CobbleRaids.captureSessionManager.removeSession(battleUUID);
      debug("creating capture pokemon before BattleBuilder");
      if (!(player.getWorld() instanceof ServerWorld serverWorld)) {
        var msg = AdventureTranslator.toNative(
          "&c[&4!&c] &cError loading the world for the capture battle.",
          CobbleRaids.language.getPrefix()
        );
        player.sendMessage(msg, false);
        cleanupFailedStart();
        return;
      }
      Pokemon pokemon = PokemonProperties.Companion.parse(raidData.getCapturePokemon()).create();
      markCapturePokemon(pokemon);
      pokemonEntity = pokemon.sendOut(
        serverWorld,
        player.getPos().add(1, 0, 1),
        null,
        entity -> {
          markCapturePokemon(entity.getPokemon());
          entity.setAiDisabled(true);
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
      pokemonEntity.heal(pokemonEntity.getMaxHealth());
      pokemonEntity.getPokemon().heal();

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

      debug("calling BattleBuilder for capture");
      var startBattle = BattleBuilder.INSTANCE.pve(
        player,
        pokemonEntity,
        leader.getUuid(),
        BattleFormat.Companion.getGEN_9_SINGLES(),
        false,
        true,
        CAPTURE_FLEE_DISTANCE,
        party
      );

      startBattle.ifSuccessful(pokemonBattle -> {
        this.battleUUID = pokemonBattle.getBattleId();
        CobbleRaids.captureSessionManager.getActiveSessions().put(this.battleUUID, this);
        CobbleRaids.captureSessionManager.getPlayerSessions().put(player.getUuid(), this);
        this.started = true;
        this.starting = false;
        debug("capture battle started successfully");
        return Unit.INSTANCE;
      });
      startBattle.ifErrored(erroredBattleStart -> {
        starting = false;
        CobbleRaids.LOGGER.error(
          CobbleRaids.MOD_ID,
          "CAPTURE_SESSION: Error starting capture battle for player " + player.getName().getString() + ": " + describeBattleErrors(erroredBattleStart.getErrors())
        );
        var msg = AdventureTranslator.toNative(
          "&c[&4!&c] &cError starting the session.",
          CobbleRaids.language.getPrefix()
        );
        player.sendMessage(msg, false);
        cleanupFailedStart();
        return Unit.INSTANCE;
      });
      } catch (Exception e) {
        starting = false;
        CobbleRaids.LOGGER.error("CAPTURE_SESSION: Unexpected error starting capture session " + battleUUID, e);
        cleanupFailedStart();
      }
    });
  }

  public void checkTimeout() {
    if (finished) return;
    ServerPlayerEntity currentPlayer = getOnlinePlayer();
    if (currentPlayer == null) {
      finishSession(started, "player-offline");
      return;
    }
    player = currentPlayer;
    if (!started && !isPlayerInRaidArea(player)) {
      sendCaptureCancelledMessage(player);
      cancelPendingSession("left-raid-area-before-start", false);
      return;
    }
    if (started && BattleRegistry.getBattle(battleUUID) == null) {
      finishSession(false, "battle-missing");
      return;
    }
    if (started && pokemonEntity != null && !pokemonEntity.getWorld().getRegistryKey().equals(player.getWorld().getRegistryKey())) {
      finishSession(true, "player-left-capture-world");
      return;
    }
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
      finishSession(true, "capture-timeout");
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
    finishSession(true, "finish-session");
  }

  public synchronized void finishSession(boolean stopBattle, String reason) {
    finishSession(stopBattle, true, reason);
  }

  private synchronized void cancelPendingSession(String reason, boolean sendEndMessage) {
    finishSession(false, sendEndMessage, reason);
  }

  private synchronized void finishSession(boolean stopBattle, boolean sendEndMessage, String reason) {
    if (finished) return;
    finished = true;
    starting = false;
    try {
      ServerPlayerEntity currentPlayer = getOnlinePlayer();
      if (sendEndMessage && currentPlayer != null) {
        HiperMessage message = CobbleRaids.language.getMessageEndCapture();
        message.sendMessage(currentPlayer, PokemonUtils.replace(message.getRawMessage(), raidData.getCapturePokemonInstance()), CobbleRaids.language.getPrefix(), false);
      }
      debug("finishing capture session stopBattle=" + stopBattle + " reason=" + reason);
      CobbleRaids.executeOnServerThread("CaptureSessionFinish", () -> {
        if (stopBattle) {
          CobbleRaids.stopBattleSafely(battleUUID, "capture-session-" + reason);
        }
        if (pokemonEntity != null) {
          debug("discarding capture pokemon reason=" + reason);
          pokemonEntity.discard();
          pokemonEntity = null;
        }
        if (currentPlayer != null) {
          RaidBall.removeRaidBalls(currentPlayer);
        }
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
    persistentData.putString(CAPTURE_NBT_KEY, playerUUID.toString());
  }

  private void cleanupFailedStart() {
    finished = true;
    starting = false;
    started = false;
    debug("cleaning failed capture start");
    CobbleRaids.captureSessionManager.removeSession(battleUUID);
    CobbleRaids.captureSessionManager.clearPendingRaidBallCatchRate(playerUUID);
    if (pokemonEntity != null) {
      pokemonEntity.discard();
      pokemonEntity = null;
    }
    RaidBall.removeRaidBalls(player);
  }

  private String describeBattleErrors(Iterable<BattleStartError> errors) {
    StringBuilder builder = new StringBuilder();
    for (BattleStartError error : errors) {
      if (builder.length() > 0) {
        builder.append(", ");
      }
      builder.append(error);
    }
    return builder.isEmpty() ? "unknown error" : builder.toString();
  }

  private ServerPlayerEntity getOnlinePlayer() {
    var currentServer = CobbleRaids.server;
    if (currentServer == null) return player;
    var onlinePlayer = currentServer.getPlayerManager().getPlayer(playerUUID);
    if (onlinePlayer != null) {
      player = onlinePlayer;
    }
    return onlinePlayer;
  }

  private boolean isPlayerInRaidArea(ServerPlayerEntity currentPlayer) {
    if (currentPlayer == null || raidData == null || raidData.getCategoryRaid() == null) return false;
    var categoryRaid = raidData.getCategoryRaid();
    ServerWorld raidWorld = categoryRaid.getWorldInstance();
    if (raidWorld == null) return false;
    if (!currentPlayer.getWorld().getRegistryKey().equals(raidWorld.getRegistryKey())) return false;
    double captureRadius = Math.max(64D, categoryRaid.getRadio() + 16D);
    return currentPlayer.getPos().isInRange(categoryRaid.getCoords().getVec3d(), captureRadius);
  }

  private void sendCaptureCancelledMessage(ServerPlayerEntity currentPlayer) {
    if (currentPlayer == null) return;
    Text msg = AdventureTranslator.toNative(
      "&c[&4!&c] &cCapture session cancelled because you left the raid area.",
      CobbleRaids.language.getPrefix()
    );
    currentPlayer.sendMessage(msg, false);
  }

  private void debug(String message) {
    if (!CobbleRaids.config.isDebug()) return;
    String playerInfo = player == null
      ? "player=null"
      : "player=" + player.getName().getString() + "/" + playerUUID + " playerWorld=" + player.getWorld().getRegistryKey().getValue();
    String pokemonInfo = pokemonEntity == null
      ? "pokemon=null"
      : "pokemon=" + pokemonEntity.getUuid() + " entityId=" + pokemonEntity.getId() + " pokemonWorld=" + pokemonEntity.getWorld().getRegistryKey().getValue();
    String categoryId = raidData == null || raidData.getCategoryRaid() == null ? "unknown" : raidData.getCategoryRaid().getId();
    CobbleRaids.LOGGER.info(
      CobbleRaids.MOD_ID,
      "CAPTURE_SESSION: " + message +
        " battleId=" + battleUUID +
        " category=" + categoryId +
        " starting=" + starting +
        " started=" + started +
        " finished=" + finished +
        " thread=" + Thread.currentThread().getName() +
        " " + playerInfo +
        " " + pokemonInfo
    );
  }
}
