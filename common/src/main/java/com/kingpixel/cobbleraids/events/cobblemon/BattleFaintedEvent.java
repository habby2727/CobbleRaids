package com.kingpixel.cobbleraids.events.cobblemon;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.kingpixel.cobbleraids.CobbleRaids;
import kotlin.Unit;

/**
 * @author Carlos Varas Alonso - 15/10/2025 21:23
 */
public class BattleFaintedEvent {
  public static void register() {
    CobblemonEvents.BATTLE_FAINTED.subscribe(Priority.HIGHEST, evt -> {
      try {
        var pokemonKilled = evt.getKilled();
        var battle = evt.getBattle();
        var pokemonEntity = pokemonKilled.getEntity();
        if (pokemonEntity == null) return Unit.INSTANCE;
        var captureSession = CobbleRaids.captureSessionManager.finishSession(battle.getBattleId());
        if (captureSession != null) return Unit.INSTANCE;
        var fight = CobbleRaids.raidManager.getFightingData(battle.getBattleId());
        if (fight == null) return Unit.INSTANCE;
        if (!fight.getPokemonEntity().equals(pokemonEntity)) return Unit.INSTANCE;
        var raid = fight.getRaid();
        if (raid == null) return Unit.INSTANCE;
        raid.updateHealth(fight, pokemonKilled.getMaxHealth());
      } catch (Exception e) {
        CobbleRaids.LOGGER.error(CobbleRaids.MOD_ID, "Error in BATTLE_FAINTED event: " + e.getMessage());
        e.printStackTrace();
      }
      return Unit.INSTANCE;
    });
  }
}
