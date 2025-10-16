package com.kingpixel.cobbleraids.events.cobblemon;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.CobbleUtils;
import kotlin.Unit;

/**
 * @author Carlos Varas Alonso - 13/09/2025 5:09
 */
public class PokeBallCaptureCompletedEvent {
  public static void register() {
    CobblemonEvents.POKE_BALL_CAPTURE_CALCULATED.subscribe(Priority.HIGHEST, evt -> {
      try {
        var battleId = evt.getPokemonEntity().getBattleId();
        if (battleId == null) return Unit.INSTANCE;
        var captureSession = CobbleRaids.captureSessionManager.getActiveSessions().get(battleId);
        if (captureSession == null) return Unit.INSTANCE;
        if (evt.getCaptureResult().isSuccessfulCapture()) {
          captureSession.setTime();
          CobbleRaids.rewardsManager.giveRewardCapture(captureSession);
        }
      } catch (Exception e) {
        CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "Error in POKE_BALL_CAPTURE_CALCULATED event: " + e.getMessage());
        e.printStackTrace();
      }
      return Unit.INSTANCE;
    });


  }
}
