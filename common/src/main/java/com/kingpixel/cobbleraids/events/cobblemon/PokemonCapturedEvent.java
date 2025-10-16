package com.kingpixel.cobbleraids.events.cobblemon;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.CaptureSessionData;
import com.kingpixel.cobbleutils.CobbleUtils;
import kotlin.Unit;

/**
 * @author Carlos Varas Alonso - 15/10/2025 21:26
 */
public class PokemonCapturedEvent {
  public static void register() {
    CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.HIGHEST, evt -> {
      try {
        var pokemon = evt.getPokemon();
        var persistentData = pokemon.getPersistentData();
        for (String key : CaptureSessionData.REMOVE_PERSISTENT_DATA) {
          persistentData.remove(key);
        }
      } catch (Exception e) {
        CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "Error in POKEMON_CAPTURED event: " + e.getMessage());
        e.printStackTrace();
      }
      return Unit.INSTANCE;
    });

  }
}
