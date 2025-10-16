package com.kingpixel.cobbleraids.events.cobblemon;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.kingpixel.cobbleraids.CobbleRaids;
import kotlin.Unit;

/**
 * @author Carlos Varas Alonso - 15/10/2025 21:16
 */
public class PokeSentEvent {
  public static void register() {
    CobblemonEvents.POKEMON_SENT_POST.subscribe(Priority.HIGHEST, evt -> {
      var pokemonEntity = evt.getPokemonEntity();
      var battleId = pokemonEntity.getBattleId();
      if (battleId == null) return Unit.INSTANCE;
      var fight = CobbleRaids.raidManager.getFightingData(battleId);
      if (fight == null) return Unit.INSTANCE;
      fight.teleportPokemon(pokemonEntity);
      return Unit.INSTANCE;
    });
  }
}
