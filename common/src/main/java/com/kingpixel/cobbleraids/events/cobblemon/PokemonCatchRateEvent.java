package com.kingpixel.cobbleraids.events.cobblemon;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.CaptureSessionData;
import kotlin.Unit;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Applies the configured RaidBall catch rate during capture sessions.
 *
 * @author Carlos Varas Alonso - 31/05/2026 21:09
 */
public class PokemonCatchRateEvent {
  public static void register() {
    CobblemonEvents.POKEMON_CATCH_RATE.subscribe(Priority.HIGHEST, evt -> {
      try {
        var pokemonEntity = evt.getPokemonEntity();
        var battleId = pokemonEntity.getBattleId();
        if (battleId == null) return Unit.INSTANCE;

        var captureSession = CobbleRaids.captureSessionManager.getActiveSessions().get(battleId);
        if (captureSession == null) return Unit.INSTANCE;

        var persistentData = pokemonEntity.getPokemon().getPersistentData();
        if (!persistentData.contains(CaptureSessionData.CAPTURE_NBT_KEY)) return Unit.INSTANCE;

        captureSession.addTime();

        if (!(evt.getThrower() instanceof ServerPlayerEntity player)) return Unit.INSTANCE;
        if (!captureSession.getPlayerUUID().equals(player.getUuid())) return Unit.INSTANCE;

        if (!CobbleRaids.config.isRaidBallEnabled()) {
          CobbleRaids.captureSessionManager.clearPendingRaidBallCatchRate(player.getUuid());
          return Unit.INSTANCE;
        }

        var catchRate = CobbleRaids.captureSessionManager.consumePendingRaidBallCatchRate(player.getUuid());
        if (catchRate != null) {
          evt.setCatchRate(catchRate);
        }
      } catch (Exception e) {
        CobbleRaids.LOGGER.error(CobbleRaids.MOD_ID, "Error in POKEMON_CATCH_RATE event: " + e.getMessage());
        e.printStackTrace();
      }
      return Unit.INSTANCE;
    });
  }
}
