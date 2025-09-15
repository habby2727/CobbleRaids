package com.kingpixel.cobbleraids.events;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.CaptureSessionData;
import com.kingpixel.cobbleraids.models.Raid;
import com.kingpixel.cobbleutils.CobbleUtils;
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
    CobblemonEvents.POKE_BALL_CAPTURE_CALCULATED.subscribe(Priority.HIGHEST, evt -> {
      var battleId = evt.getPokemonEntity().getBattleId();
      if (battleId == null) return Unit.INSTANCE;
      var captureSession = CobbleRaids.captureSessionManager.getActiveSessions().get(battleId);
      if (captureSession == null) return Unit.INSTANCE;
      if (evt.getCaptureResult().isSuccessfulCapture()) {
        captureSession.setTime();
        CobbleRaids.rewardsManager.giveRewardCapture(captureSession);
      }
      return Unit.INSTANCE;
    });

    CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.HIGHEST, evt -> {
      var pokemon = evt.getPokemon();
      var persistentData = pokemon.getPersistentData();
      for (String key : CaptureSessionData.REMOVE_PERSISTENT_DATA) {
        persistentData.remove(key);
      }
      return Unit.INSTANCE;
    });

    CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.HIGHEST, evt -> {
      var battle = evt.getBattle();
      var battleId = battle.getBattleId();
      var fight = CobbleRaids.raidManager.getFightingData(battleId);
      var captureSession = CobbleRaids.captureSessionManager.finishSession(battleId);
      if (captureSession != null) {
        captureSession.finishSession();
      }
      if (fight != null) {
        fight.stop();
      }
      return Unit.INSTANCE;
    });

    CobblemonEvents.BATTLE_FLED.subscribe(Priority.HIGHEST, evt -> {
      var battle = evt.getBattle();
      var battleId = battle.getBattleId();

      var captureSession = CobbleRaids.captureSessionManager.finishSession(battleId);
      if (captureSession != null) {
        if (CobbleRaids.config.isDebug()) {
          CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "BATTLE_FLED: A player has fled a capture session.");
        }
        return Unit.INSTANCE;
      }
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
      var captureSession = CobbleRaids.captureSessionManager.finishSession(battle.getBattleId());
      if (captureSession != null) {
        if (CobbleRaids.config.isDebug()) {
          CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "BATTLE_FLED: A player has fled a capture session.");
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

    CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.LOWEST, evt -> {
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
