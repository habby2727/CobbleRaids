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
import com.kingpixel.cobbleraids.managers.BattleManager;
import com.kingpixel.cobbleraids.model.RaidStarted;
import kotlin.Unit;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 17/01/2025 23:36
 */
public class BattleEvents {
  public static void register() {
    // Antes de empezar un combate
    CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.HIGH, (evt) -> {
      try {
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
          evt.setReason(Text.empty());
          RaidStarted raidStarted = BattleManager.getActiveRaid(pokemonEntity.getUuid());
          if (raidStarted == null) {
            pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
            evt.cancel();
            return Unit.INSTANCE;
          }
          if (!raidStarted.getPokemonRaid().isPermitted(player)) {
            evt.cancel();
            return Unit.INSTANCE;
          }
          raidStarted.startBattle(player);
          evt.cancel();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return Unit.INSTANCE;
    });


    CobblemonEvents.LOOT_DROPPED.subscribe(Priority.HIGHEST, evt -> {
      var livingEntity = evt.getEntity();
      if (livingEntity == null) return Unit.INSTANCE;
      if (livingEntity instanceof PokemonEntity pokemonEntity) {
        Pokemon pokemon = pokemonEntity.getPokemon();
        NbtCompound nbt = pokemon.getPersistentData();
        if (nbt.getBoolean(CobbleRaids.TAG_RAID) || nbt.getBoolean(CobbleRaids.TAG_FAKERAID)) {
          pokemon.removeHeldItem();
          evt.cancel();
        }
      }
      return Unit.INSTANCE;
    });

    CobblemonEvents.BATTLE_FLED.subscribe(Priority.HIGHEST, evt -> {
      for (BattleActor actor : evt.getBattle().getActors()) {
        if (actor == null) continue;
        if (actor instanceof PokemonBattleActor pokemonBattleActor) {
          PokemonEntity pokemonEntity = pokemonBattleActor.getEntity();
          if (pokemonEntity == null) continue;
          Pokemon pokemon = pokemonEntity.getPokemon();
          if (pokemon == null) continue;
          if (pokemon.getPersistentData().getBoolean(CobbleRaids.TAG_FAKERAID)) {
            pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
          }
        }
      }
      return Unit.INSTANCE;
    });

    // Victoria en un combate
    CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.HIGHEST, evt -> {
      ServerPlayerEntity player = null;
      var winners = evt.getWinners().stream().toList();
      for (BattleActor winner : winners) {
        for (BattleActor actor : winner.getBattle().getActors()) {
          if (actor instanceof PlayerBattleActor playerBattleActor) {
            player = playerBattleActor.getEntity();
          }
        }
      }
      if (player == null) return Unit.INSTANCE;
      for (BattleActor loser : evt.getLosers()) {
        var battle = loser.getBattle();
        for (BattleActor actor : battle.getActors()) {
          if (actor instanceof PokemonBattleActor pokemonBattleActor) {
            Pokemon pokemon = pokemonBattleActor.getPokemon().getOriginalPokemon();
            if (pokemon.getPersistentData().getBoolean(CobbleRaids.TAG_FAKERAID)) {
              UUID uuidBattle = pokemon.getPersistentData().getUuid(CobbleRaids.TAG_RAID_ACTIVE);
              RaidStarted raidStarted = BattleManager.getActiveRaid(uuidBattle);
              if (raidStarted != null) {
                raidStarted.finishBattle(player, pokemon);
              }
            }
          }
        }
      }
      return Unit.INSTANCE;
    });


  }

}
