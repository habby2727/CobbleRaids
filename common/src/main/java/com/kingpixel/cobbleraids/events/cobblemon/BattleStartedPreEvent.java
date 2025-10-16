package com.kingpixel.cobbleraids.events.cobblemon;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.Raid;
import com.kingpixel.cobbleutils.CobbleUtils;
import kotlin.Unit;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 15/10/2025 21:23
 */
public class BattleStartedPreEvent {
  public static void register() {
    CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.HIGHEST, evt -> {
      try {
        var battle = evt.getBattle();
        String categoryId = null;
        var actors = battle.getActors();
        PlayerBattleActor playerBattleActor = null;
        ServerPlayerEntity player = battle.getPlayers().getFirst();
        for (BattleActor actor : actors) {
          if (actor == null) continue;
          if (actor instanceof PlayerBattleActor pActor) {
            playerBattleActor = pActor;
            continue;
          }
          if (!(actor instanceof PokemonBattleActor pokemonBattleActor)) continue;
          var battlePokemon = pokemonBattleActor.getPokemon();
          var pokemonEntity = battlePokemon.getEntity();
          if (pokemonEntity == null) continue;
          var pokemon = pokemonEntity.getPokemon();
          var persistentData = pokemon.getPersistentData();
          categoryId = persistentData.getString(Raid.RAID_CATEGORY_NBT_KEY);
          if (categoryId != null && !categoryId.isEmpty()) break;
        }

        if (categoryId != null && !categoryId.isEmpty()) {
          var categoryRaid = CobbleRaids.categorys.getCategory(categoryId);
          if (categoryRaid == null) {
            if (CobbleRaids.config.isDebug()) {
              CobbleUtils.LOGGER.warn(CobbleRaids.MOD_ID, "BATTLE_STARTED_PRE: CategoryRaid is null for categoryId " + categoryId);
            }
            return Unit.INSTANCE;
          }
          if (playerBattleActor == null) {
            if (CobbleRaids.config.isDebug()) {
              CobbleUtils.LOGGER.warn(CobbleRaids.MOD_ID, "BATTLE_STARTED_PRE: PlayerBattleActor is null.");
            }
            return Unit.INSTANCE;
          }
          if (CobbleRaids.config.isDebug()) {
            CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "BATTLE_STARTED_PRE: Player " + player.getName().getString() +
              " is starting a raid battle of category " + categoryId);
          }
          var list = playerBattleActor.getPokemonList();
          for (BattlePokemon battlePokemon : list) {
            battlePokemon.setGone(false);
          }
          list.removeIf(battlePokemon -> categoryRaid.isBlackList(battlePokemon.getOriginalPokemon()));
          if (list.isEmpty()) {
            evt.setReason(Text.literal(
              "You don't have any Pokémons that can fight this raid. Please check the blacklist configured."
            ));
            evt.cancel();
            return Unit.INSTANCE;
          }
          return Unit.INSTANCE;
        } else {
          if (CobbleRaids.config.isDebug()) {
            CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "BATTLE_STARTED_PRE: No categoryId found, normal battle.");
          }
        }


        // Find the raid UUID from the participating pokemons
        UUID raidUUID = null;
        PokemonEntity pokemonEntity = null;
        for (BattleActor actor : actors) {
          if (!(actor instanceof PokemonBattleActor activeBattlePokemon)) continue;
          var battlePokemon = activeBattlePokemon.getPokemon();
          pokemonEntity = battlePokemon.getEntity();
          if (pokemonEntity == null) continue;
          var pokemon = pokemonEntity.getPokemon();
          var persistentData = pokemon.getPersistentData();
          if (!persistentData.contains(Raid.RAID_NBT_KEY)) continue;
          raidUUID = persistentData.getUuid(Raid.RAID_NBT_KEY);
          if (raidUUID != null) break;
        }
        if (raidUUID == null) return Unit.INSTANCE;
        if (player == null) return Unit.INSTANCE;

        evt.setReason(null);
        evt.cancel();
        var raid = CobbleRaids.raidManager.getRaid(raidUUID);
        if (raid == null) {
          pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
          return Unit.INSTANCE;
        }
        raid.openStartBattleMenu(player);
        return Unit.INSTANCE;
      } catch (Exception e) {
        CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "Error in BATTLE_STARTED_PRE event: " + e.getMessage());
        e.printStackTrace();
        return Unit.INSTANCE;
      }
    });
  }
}
