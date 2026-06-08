package com.kingpixel.cobbleraids.manager;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.CaptureSessionData;
import com.kingpixel.cobbleraids.models.Raid;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Carlos Varas Alonso - 13/09/2025 21:35
 */
@Data
public class CaptureSessionManager {
  // UUID Battle - CaptureSessionData
  private ConcurrentHashMap<UUID, CaptureSessionData> activeSessions = new ConcurrentHashMap<>();
  // UUID Player - CaptureSessionData
  private ConcurrentHashMap<UUID, CaptureSessionData> playerSessions = new ConcurrentHashMap<>();
  // UUID Player - RaidBall catch rate prepared on item use
  private ConcurrentHashMap<UUID, Float> pendingRaidBallCatchRates = new ConcurrentHashMap<>();

  public void startSession(ServerPlayerEntity player, Raid raid) {
    var previousSession = playerSessions.remove(player.getUuid());
    if (previousSession != null) {
      activeSessions.remove(previousSession.getBattleUUID());
      previousSession.finishSession(true, "replaced-by-new-session");
    }
    var session = new CaptureSessionData(raid, player);
    activeSessions.put(session.getBattleUUID(), session);
    playerSessions.put(player.getUuid(), session);
  }

  public CaptureSessionData finishSession(UUID battleUUID) {
    return finishSession(battleUUID, false, "battle-event");
  }

  public CaptureSessionData stopSession(UUID battleUUID, String reason) {
    return finishSession(battleUUID, true, reason);
  }

  public CaptureSessionData finishSession(UUID battleUUID, boolean stopBattle, String reason) {
    if (CobbleRaids.config.isDebug()) {
      CobbleRaids.LOGGER.info("Trying to finish capture session for battle UUID: " + battleUUID + " stopBattle=" + stopBattle + " reason=" + reason);
    }
    var session = activeSessions.remove(battleUUID);
    if (session != null) {
      playerSessions.remove(session.getPlayerUUID());
      clearPendingRaidBallCatchRate(session.getPlayerUUID());
      if (CobbleRaids.config.isDebug()) {
        CobbleRaids.LOGGER.info("Capture session found and removed for battle UUID: " + battleUUID);
      }
      session.finishSession(stopBattle, reason);
    } else if (CobbleRaids.config.isDebug()) {
      CobbleRaids.LOGGER.info("No active capture session found for battle UUID: " + battleUUID);
    }
    return session;
  }

  public void finishAllSessions() {
    for (var session : activeSessions.values()) {
      session.finishSession(true, "finish-all-sessions");
    }
  }

  public void finishSessionPlayer(UUID playerUUID) {
    if (CobbleRaids.config.isDebug()) {
      CobbleRaids.LOGGER.info("Finishing capture session for player UUID: " + playerUUID);
    }
    var session = playerSessions.remove(playerUUID);
    clearPendingRaidBallCatchRate(playerUUID);
    if (session != null) {
      activeSessions.remove(session.getBattleUUID());
      session.finishSession(true, "player-session-finished");
    }
  }

  public void removeSession(UUID battleUUID) {
    if (CobbleRaids.config.isDebug()) {
      CobbleRaids.LOGGER.info("Removing capture session mapping for battle UUID: " + battleUUID);
    }
    var session = activeSessions.remove(battleUUID);
    if (session != null) {
      playerSessions.remove(session.getPlayerUUID());
      clearPendingRaidBallCatchRate(session.getPlayerUUID());
    }
  }

  public CaptureSessionData getSessionByPlayer(UUID playerUUID) {
    return playerSessions.get(playerUUID);
  }

  public void setPendingRaidBallCatchRate(UUID playerUUID, float catchRate) {
    pendingRaidBallCatchRates.put(playerUUID, catchRate);
  }

  public Float consumePendingRaidBallCatchRate(UUID playerUUID) {
    return pendingRaidBallCatchRates.remove(playerUUID);
  }

  public void clearPendingRaidBallCatchRate(UUID playerUUID) {
    pendingRaidBallCatchRates.remove(playerUUID);
  }
}
