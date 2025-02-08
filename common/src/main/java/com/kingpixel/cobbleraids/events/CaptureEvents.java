package com.kingpixel.cobbleraids.events;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.PokemonPropertyExtractor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.model.CaptureSession;
import com.kingpixel.cobbleraids.model.Raid;
import kotlin.Unit;
import net.minecraft.entity.Entity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 18/01/2025 22:06
 */
public class CaptureEvents {
  public static void register() {
    CobblemonEvents.BATTLE_FLED.subscribe(Priority.LOWEST, (evt) -> {
      var battle = evt.getBattle();
      handle(battle);
      return Unit.INSTANCE;
    });
    CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.LOWEST, (evt) -> {
      var battle = evt.getBattle();
      handle(battle);
      return Unit.INSTANCE;
    });

    CobblemonEvents.BATTLE_FAINTED.subscribe(Priority.LOWEST, (evt) -> {
      var battle = evt.getBattle();
      handle(battle);
      return Unit.INSTANCE;
    });

    CobblemonEvents.THROWN_POKEBALL_HIT.subscribe(Priority.HIGHEST, (evt) -> {
      try {
        var pokemon = evt.getPokemon().getPokemon();
        if (Raid.isRaid(pokemon) && CobbleRaids.config.isNeedPokeBallRaids()) {

        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return Unit.INSTANCE;
    });

    CobblemonEvents.POKEMON_CATCH_RATE.subscribe(Priority.LOWEST, (evt) -> {
      try {
        var pokemonEntity = evt.getPokemonEntity();
        var pokemon = pokemonEntity.getPokemon();
        var emptyPokeBallEntity = evt.getPokeBallEntity();
        if (Raid.isRaid(pokemon)) {
          CaptureSession.capturing(pokemon.getUuid());
          return Unit.INSTANCE;
        }
        /*if (CobbleRaids.config.isNeedPokeBallRaids()) {
          var custom_data =
            emptyPokeBallEntity.getPokeBall().item().getComponents().get(DataComponentTypes.CUSTOM_DATA).getNbt();
          var raidPokeball = custom_data.getString("raidPokeball");
          if (raidPokeball.isEmpty()) {
            evt.setCatchRate(0);
            if (CobbleRaids.config.isDebug()) {
              CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID + " - Error: Raid Pokeball is empty");
            }
            return Unit.INSTANCE;
          } else {
            if (CobbleRaids.config.isDebug()) {
              CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID + " - Raid Pokeball: " + raidPokeball);
            }
            evt.setCatchRate(custom_data.getInt("catchRate"));
            CaptureSession.capturing(pokemon.getUuid());
          }
        }*/
      } catch (Exception e) {
        e.printStackTrace();
      }

      return Unit.INSTANCE;
    });

    CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, (evt) -> {
      try {
        var pokemon = evt.getPokemon();
        var nbt = pokemon.getPersistentData();
        if (nbt.getBoolean(CobbleRaids.TAG_RAID_CAPTURE)) {
          pokemon.createPokemonProperties(
            List.of(
              PokemonPropertyExtractor.NATURE,
              PokemonPropertyExtractor.IVS,
              PokemonPropertyExtractor.EVS,
              PokemonPropertyExtractor.ABILITY
            )
          );
          pokemon.setLevel(1);
          CaptureSession.removeUuid(pokemon.getUuid());
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return Unit.INSTANCE;
    });
  }


  private static void handle(PokemonBattle battle) {
    for (BattleActor actor : battle.getActors()) {
      if (actor instanceof PokemonBattleActor pokemonBattleActor) {
        var pokemonEntity = pokemonBattleActor.getEntity();
        if (pokemonEntity == null) continue;
        var pokemon = pokemonEntity.getPokemon();
        if (pokemon.getPersistentData().getBoolean(CobbleRaids.TAG_RAID_CAPTURE)) {
          CaptureSession.removeUuid(pokemonEntity.getPokemon().getUuid());
          pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
          return;
        }
      }
    }
  }
}
