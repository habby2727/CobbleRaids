package com.kingpixel.cobbleraids.events;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.model.CaptureSession;
import com.kingpixel.cobbleraids.model.Raid;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import kotlin.Unit;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 18/01/2025 22:06
 */
public class CaptureEvents {
  public static void register() {
    // Battle events
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

    // Capture events
    CobblemonEvents.POKEMON_CATCH_RATE.subscribe(Priority.HIGHEST, (evt) -> {
      try {
        var livingEntity = evt.getThrower();
        var pokemonEntity = evt.getPokemonEntity();
        var pokemon = pokemonEntity.getPokemon();
        if (Raid.isRaid(pokemon)) {
          String id = evt.getPokeBallEntity().getPokeBall().item().getTranslationKey()
            .replace("item.", "")
            .replace(".", ":");
          if (!CobbleRaids.config.getPokeballs().contains(id)) {
            evt.setCatchRate(0);
            if (livingEntity instanceof ServerPlayerEntity player) {
              PlayerUtils.sendMessage(
                player,
                CobbleRaids.language.getMessageEnabledPokeBalls()
                  .replace("%pokeballs%", String.join(", ", CobbleRaids.config.getPokeballs())),
                CobbleRaids.config.getPrefix(),
                TypeMessage.CHAT
              );
              player.getInventory().insertStack(evt.getPokeBallEntity().getPokeBall().item().getDefaultStack());
            }
          }
          CaptureSession.capturing(pokemon.getUuid());
        }
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
          nbt.remove(CobbleRaids.TAG_RAID_CAPTURE);
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
