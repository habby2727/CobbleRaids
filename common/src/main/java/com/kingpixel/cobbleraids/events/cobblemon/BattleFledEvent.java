package com.kingpixel.cobbleraids.events.cobblemon;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.kingpixel.cobbleraids.CobbleRaids;
import kotlin.Unit;

/**
 * @author Carlos Varas Alonso - 15/10/2025 21:24
 */
public class BattleFledEvent {
  public static void register() {
    CobblemonEvents.BATTLE_FLED.subscribe(Priority.HIGHEST, evt -> {
      try {
        var battle = evt.getBattle();
        var battleId = battle.getBattleId();

        var captureSession = CobbleRaids.captureSessionManager.finishSession(battleId);
        if (captureSession != null) {
          if (CobbleRaids.config.isDebug()) {
            CobbleRaids.LOGGER.info(CobbleRaids.MOD_ID, "BATTLE_FLED: A player has fled a capture session.");
          }
          return Unit.INSTANCE;
        }
        var fight = CobbleRaids.raidManager.getFightingData(battleId);
        if (fight == null) {
          if (CobbleRaids.config.isDebug()) {
            CobbleRaids.LOGGER.warn(CobbleRaids.MOD_ID, "BATTLE_FLED: A player has fled but the fight is null.");
          }
          return Unit.INSTANCE;
        }
        if (CobbleRaids.config.isDebug()) {
          CobbleRaids.LOGGER.info(CobbleRaids.MOD_ID, "BATTLE_FLED: A player has fled the raid battle.");
        }
        fight.stop(false);
      } catch (Exception e) {
        CobbleRaids.LOGGER.error(CobbleRaids.MOD_ID, "Error in BATTLE_FLED event: " + e.getMessage());
        e.printStackTrace();
      }
      return Unit.INSTANCE;
    });

  }
}
