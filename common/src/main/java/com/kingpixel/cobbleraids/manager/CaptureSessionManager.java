package com.kingpixel.cobbleraids.manager;

import com.kingpixel.cobbleraids.models.CaptureSessionData;
import com.kingpixel.cobbleraids.models.Raid;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Carlos Varas Alonso - 13/09/2025 21:35
 */
@Data
public class CaptureSessionManager {
  // UUID Battle - CaptureSessionData
  private Map<UUID, CaptureSessionData> activeSessions = new ConcurrentHashMap<>();

  public void startSession(ServerPlayerEntity player, Raid raid) {
    var session = new CaptureSessionData(raid, player);
    activeSessions.put(session.getBattleUUID(), session);
  }

  public CaptureSessionData finishSession(UUID battleUUID) {
    var session = activeSessions.remove(battleUUID);
    if (session != null) session.finishSession();
    return session;
  }

  public void finishAllSessions() {
    for (var session : activeSessions.values()) {
      session.finishSession();
    }
  }

  public void finishSessionPlayer(UUID uuid) {
    var session = activeSessions.values().stream().filter(s -> s.getPlayer().getUuid().equals(uuid)).findFirst();
    session.ifPresent(s -> {
      activeSessions.remove(s.getBattleUUID());
      s.finishSession();
    });
  }

  public void removeSession(UUID battleUUID) {
    activeSessions.remove(battleUUID);
  }
}
