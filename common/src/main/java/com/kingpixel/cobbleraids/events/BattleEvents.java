package com.kingpixel.cobbleraids.events;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.Raid;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import kotlin.Unit;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 13/09/2025 5:09
 */
public class BattleEvents {
  public static void register() {

    CobblemonEvents.BATTLE_FLED.subscribe(Priority.LOWEST, evt -> {
      var battle = evt.getBattle();
      var battleId = battle.getBattleId();
      var fight = CobbleRaids.raidManager.getFightingData(battleId);
      if (fight == null) {
        if (CobbleRaids.config.isDebug()) {
          CobbleUtils.LOGGER.warn(CobbleRaids.MOD_ID, "BATTLE_FLED: A player has fled but the fight is null.");
        }
        return Unit.INSTANCE;
      }
      if (CobbleRaids.config.isDebug()) {
        CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "BATTLE_FLED: A player has fled the raid battle.");
      }
      fight.stop();
      return Unit.INSTANCE;
    });

    CobblemonEvents.BATTLE_FAINTED.subscribe(Priority.HIGHEST, evt -> {
      var pokemonKilled = evt.getKilled();
      var battle = evt.getBattle();
      var pokemonEntity = pokemonKilled.getEntity();
      if (pokemonEntity == null) {
        if (CobbleRaids.config.isDebug()) {
          CobbleUtils.LOGGER.warn(CobbleRaids.MOD_ID, "BATTLE_FAINTED: A pokemon has fainted but the pokemonEntity is null.");
        }
        return Unit.INSTANCE;
      }
      var fight = CobbleRaids.raidManager.getFightingData(battle.getBattleId());
      if (fight == null) {
        if (CobbleRaids.config.isDebug()) {
          CobbleUtils.LOGGER.warn(CobbleRaids.MOD_ID, "BATTLE_FAINTED: A pokemon has fainted but the fight is null.");
        }
        return Unit.INSTANCE;
      }
      if (!fight.getPokemonEntity().equals(pokemonEntity)) {
        if (CobbleRaids.config.isDebug()) {
          CobbleUtils.LOGGER.warn(CobbleRaids.MOD_ID, "BATTLE_FAINTED: A pokemon has fainted but it is not the raid " +
            "pokemon.");
        }
        return Unit.INSTANCE;
      }
      var raid = fight.getRaid();
      if (raid == null) {
        if (CobbleRaids.config.isDebug()) {
          CobbleUtils.LOGGER.warn(CobbleRaids.MOD_ID, "BATTLE_FAINTED: A pokemon has fainted but the raid is null.");
        }
        return Unit.INSTANCE;
      }
      raid.updateHealth(fight, pokemonKilled.getMaxHealth());
      return Unit.INSTANCE;
    });

    CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.HIGHEST, evt -> {
      var battle = evt.getBattle();
      UUID battleId = battle.getBattleId();
      var existingFight = CobbleRaids.raidManager.getFightingData(battleId);
      if (existingFight != null) {
        var playerActor = battle.getActor(existingFight.getPlayer());
        if (playerActor != null) {
          playerActor.getPokemonList().removeIf(battlePokemon -> existingFight.getRaid().getCategoryRaid().isBlackList(battlePokemon.getEffectedPokemon()));
          if (playerActor.getPokemonList().isEmpty()) {
            PlayerUtils.sendMessage(
              existingFight.getPlayer(),
              "§c[§6CobbleRaids§c] §cYou don't have any valid Pokémon to fight this raid!§r",
              CobbleRaids.language.getPrefix(),
              TypeMessage.CHAT
            );
            if (CobbleRaids.config.isDebug()) {
              CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "BATTLE_STARTED_PRE: A battle has started but the player has no valid pokemons.");
            }
            evt.setReason(Text.empty());
            evt.cancel();
            existingFight.stop();
            return Unit.INSTANCE;
          }
        }
      }
      var activePokemons = battle.getActivePokemon();
      UUID raidUUID = null;
      PokemonEntity pokemonEntity = null;
      for (ActiveBattlePokemon activeBattlePokemon : activePokemons) {
        var battlePokemon = activeBattlePokemon.getBattlePokemon();
        if (battlePokemon == null) {
          if (CobbleRaids.config.isDebug()) {
            CobbleUtils.LOGGER.warn(CobbleRaids.MOD_ID, "BATTLE_STARTED_PRE: A battle has started but one of the " +
              "battlePokemons is null.");
          }
          continue;
        }
        pokemonEntity = battlePokemon.getEntity();
        if (pokemonEntity == null) {
          if (CobbleRaids.config.isDebug()) {
            CobbleUtils.LOGGER.warn(CobbleRaids.MOD_ID, "BATTLE_STARTED_PRE: A battle has started but one of the pokemonEntities is null.");
          }
          continue;
        }
        var pokemon = pokemonEntity.getPokemon();
        var persistentData = pokemon.getPersistentData();
        if (!persistentData.contains(Raid.RAID_NBT_KEY)) return Unit.INSTANCE;
        raidUUID = persistentData.getUuid(Raid.RAID_NBT_KEY);
        if (raidUUID != null) {
          if (CobbleRaids.config.isDebug()) {
            CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "BATTLE_STARTED_PRE: A battle has started and the raid UUID was found: " + raidUUID);
          }
          break;
        }
      }
      if (raidUUID == null) {
        if (CobbleRaids.config.isDebug()) {
          CobbleUtils.LOGGER.warn(CobbleRaids.MOD_ID, "BATTLE_STARTED_PRE: A battle has started but no raid UUID was found in any of the pokemons.");
        }
        return Unit.INSTANCE;
      }
      ServerPlayerEntity player = battle.getPlayers().getFirst();
      if (player == null) {
        if (CobbleRaids.config.isDebug()) {
          CobbleUtils.LOGGER.warn(CobbleRaids.MOD_ID, "BATTLE_STARTED_PRE: A battle has started but no player was found.");
        }
        return Unit.INSTANCE;
      }
      evt.setReason(Text.empty());
      evt.cancel();
      var raid = CobbleRaids.raidManager.getRaid(raidUUID);
      if (raid == null) {
        pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
        return Unit.INSTANCE;
      }
      raid.openStartBattleMenu(player);
      return Unit.INSTANCE;
    });
  }
}
