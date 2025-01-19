package com.kingpixel.cobbleraids.events;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.CobbleUtils;
import kotlin.Unit;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Carlos Varas Alonso - 17/01/2025 23:36
 */
public class BattleEvents {
  public static void register() {
    // Antes de empezar un combate
    CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.HIGHEST, (evt) -> {
      PokemonBattle battle = evt.getBattle();
      ServerPlayerEntity player = null;
      PokemonEntity pokemonEntity = null;
      for (BattleActor actor : battle.getActors()) {
        if (actor instanceof PlayerBattleActor playerBattleActor) {
          player = playerBattleActor.getEntity();
        } else if (actor instanceof PokemonBattleActor pokemonBattleActor) {
          pokemonEntity = pokemonBattleActor.getEntity();
        }
      }
      if (player == null || pokemonEntity == null) return Unit.INSTANCE;
      Pokemon pokemon = pokemonEntity.getPokemon();
      if (pokemon.getPersistentData().getBoolean(CobbleRaids.TAG_RAID)) {
        if (CobbleRaids.battleManager == null) {
          pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
          evt.cancel();
          return Unit.INSTANCE;
        }
        evt.setReason(null);
        if (!CobbleRaids.battleManager.getPokemonRaid().isPermitted(player)) {
          evt.cancel();
          return Unit.INSTANCE;
        }
        player.sendMessage(Text.literal("You are battling a raid pokemon!"), true);
        evt.cancel();
        CobbleRaids.battleManager.startBattle(player);
      }
      return Unit.INSTANCE;
    });


    CobblemonEvents.LOOT_DROPPED.subscribe(Priority.HIGHEST, evt -> {
      CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "Loot dropped");
      var livingEntity = evt.getEntity();
      if (livingEntity == null) return Unit.INSTANCE;
      if (livingEntity instanceof PokemonEntity pokemonEntity) {
        Pokemon pokemon = pokemonEntity.getPokemon();
        pokemon.clone(true);
        NbtCompound nbt = pokemon.getPersistentData();
        if (nbt.getBoolean(CobbleRaids.TAG_RAID) || nbt.getBoolean(CobbleRaids.TAG_FAKERAID)) {
          evt.cancel();
        }
      }
      return Unit.INSTANCE;
    });

    CobblemonEvents.BATTLE_FLED.subscribe(Priority.LOWEST, evt -> {
      for (BattleActor actor : evt.getBattle().getActors()) {
        if (actor instanceof PokemonBattleActor pokemonBattleActor) {
          PokemonEntity pokemonEntity = pokemonBattleActor.getEntity();
          if (pokemonEntity.getPokemon().getPersistentData().getBoolean(CobbleRaids.TAG_FAKERAID)) {
            pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
          }
        }
      }
      return Unit.INSTANCE;
    });

    // Victoria en un combate
    CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.LOWEST, evt -> {
      try {
        if (CobbleRaids.battleManager != null) {
          AtomicReference<ServerPlayerEntity> player = new AtomicReference<>();
          var winners = evt.getWinners().stream().toList();
          winners.forEach(winner -> {
            winner.getBattle().getActors().forEach(battleActor -> {
              if (battleActor instanceof PlayerBattleActor playerBattleActor) {
                player.set(playerBattleActor.getEntity());
              }
            });
          });
          if (player.get() == null) return Unit.INSTANCE;
          evt.getLosers().forEach(loser -> {
            var battle = loser.getBattle();
            battle.getActors().forEach(battleActor -> {
              if (battleActor instanceof PokemonBattleActor pokemonBattleActor) {
                Pokemon pokemon = pokemonBattleActor.getPokemon().getOriginalPokemon();
                if (pokemon.getPersistentData().getBoolean(CobbleRaids.TAG_FAKERAID)) {
                  if (CobbleRaids.battleManager != null)
                    CobbleRaids.battleManager.removeLife(player.get(), pokemon);
                }
              }
            });
          });
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return Unit.INSTANCE;
    });


  }

}
