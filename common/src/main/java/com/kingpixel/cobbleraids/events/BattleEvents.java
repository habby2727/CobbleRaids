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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;
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
      if (player == null) return Unit.INSTANCE;
      if (pokemonEntity == null) return Unit.INSTANCE;
      Pokemon pokemon = pokemonEntity.getPokemon();
      if (pokemon.getPersistentData().getBoolean(CobbleRaids.TAG_RAID)) {
        evt.setReason(Text.empty());
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

    // Huir de un combate
    CobblemonEvents.BATTLE_FLED.subscribe(Priority.LOWEST, evt -> {
      UUID battleUUID = evt.getBattle().getBattleId();
      if (CobbleRaids.battleManager != null) {
        CobbleRaids.battleManager.manageBattleFled(battleUUID);
      }
      return Unit.INSTANCE;
    });

    CobblemonEvents.LOOT_DROPPED.subscribe(Priority.HIGHEST, evt -> {
      var livingEntity = evt.getEntity();
      if (livingEntity instanceof PokemonEntity pokemonEntity) {
        Pokemon pokemon = pokemonEntity.getPokemon();
        pokemon.clone(true);
        NbtCompound nbt = pokemon.getPersistentData();
        if (nbt.getBoolean(CobbleRaids.TAG_RAID) || nbt.getBoolean(CobbleRaids.TAG_FAKERAID)) {
          if (CobbleUtils.config.isDebug()) {
            CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "Prevent loot drop");
          }
          evt.cancel();
        }
      }
      return Unit.INSTANCE;
    });

    CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.LOWEST, evt -> {
      try {
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
                if (CobbleUtils.config.isDebug()) {
                  CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID,
                    "Health: " + pokemon.getCurrentHealth() + " MaxHealth: " + pokemon.getMaxHealth());
                }
                if (CobbleRaids.battleManager != null)
                  CobbleRaids.battleManager.removeLife(player.get(), pokemon.getCurrentHealth(), pokemon.getMaxHealth());
              }
            }
          });
        });
      } catch (Exception e) {
        e.printStackTrace();
      }
      return Unit.INSTANCE;
    });

  }
}
