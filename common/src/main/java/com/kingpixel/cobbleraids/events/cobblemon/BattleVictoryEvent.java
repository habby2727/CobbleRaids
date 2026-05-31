package com.kingpixel.cobbleraids.events.cobblemon;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.kingpixel.cobbleraids.CobbleRaids;
import kotlin.Unit;

/**
 * @author Carlos Varas Alonso - 15/10/2025 21:25
 */
public class BattleVictoryEvent {
  public static void register() {
    CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.HIGHEST, evt -> {
      try {
        var battle = evt.getBattle();
        var battleId = battle.getBattleId();
        var fight = CobbleRaids.raidManager.getFightingData(battleId);
        var captureSession = CobbleRaids.captureSessionManager.finishSession(battleId);
        if (captureSession != null) {
          if (CobbleRaids.config.isDebug()) {
            CobbleRaids.LOGGER.info(CobbleRaids.MOD_ID, "BATTLE_VICTORY: A player has won a capture session.");
          }
          return Unit.INSTANCE;
        }
        if (fight != null) fight.stop(false);
        return Unit.INSTANCE;
      } catch (Exception e) {
        CobbleRaids.LOGGER.error(CobbleRaids.MOD_ID, "Error in BATTLE_VICTORY event: " + e.getMessage());
        e.printStackTrace();
      }
      return Unit.INSTANCE;
    });
  }
}
