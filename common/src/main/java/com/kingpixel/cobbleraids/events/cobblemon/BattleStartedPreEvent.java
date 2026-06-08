package com.kingpixel.cobbleraids.events.cobblemon;

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
        ServerPlayerEntity player = battle.getPlayers().isEmpty() ? null : battle.getPlayers().getFirst();
        boolean isCaptureSessionBattle = false;
        debugBattle("received BATTLE_STARTED_PRE", battle.getBattleId(), player, actors, categoryId, false);

        for (BattleActor actor : actors) {
          if (actor instanceof PlayerBattleActor pActor) {
            playerBattleActor = pActor;
            continue;
          }
          if (!(actor instanceof PokemonBattleActor pokemonBattleActor)) continue;
          var battlePokemon = pokemonBattleActor.getPokemon();
          var pokemonEntity = battlePokemon.getEntity();
          if (pokemonEntity == null) continue;
          var persistentData = pokemonEntity.getPokemon().getPersistentData();
          if (persistentData.contains(CaptureSessionData.CAPTURE_NBT_KEY)) {
            isCaptureSessionBattle = true;
          }
          if (categoryId != null && !categoryId.isEmpty()) continue;
          String currentCategoryId = persistentData.getString(Raid.RAID_CATEGORY_NBT_KEY);
          if (currentCategoryId != null && !currentCategoryId.isEmpty()) {
            categoryId = currentCategoryId;
          }
        }

        if (isCaptureSessionBattle) {
          debugBattle("capture session battle detected; skipping raid pre-start handling", battle.getBattleId(), player, actors, categoryId, true);
          return Unit.INSTANCE;
        }

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
          if (categoryId == null || categoryId.isEmpty()) {
            categoryId = persistentData.getString(Raid.RAID_CATEGORY_NBT_KEY);
          }
          if (categoryId != null && !categoryId.isEmpty()) break;
        }

        if (categoryId != null && !categoryId.isEmpty()) {
          var categoryRaid = CobbleRaids.categorys.getCategory(categoryId);
          if (categoryRaid == null) {
            if (CobbleRaids.config.isDebug()) {
              CobbleRaids.LOGGER.warn("BATTLE_STARTED_PRE: CategoryRaid is null for categoryId " + categoryId);
            }
            return Unit.INSTANCE;
          }
          if (playerBattleActor == null) {
            if (CobbleRaids.config.isDebug()) {
              CobbleRaids.LOGGER.warn("BATTLE_STARTED_PRE: PlayerBattleActor is null.");
            }
            return Unit.INSTANCE;
          }
          if (CobbleRaids.config.isDebug()) {
            String playerName = player == null ? "unknown" : player.getName().getString();
            CobbleRaids.LOGGER.info("BATTLE_STARTED_PRE: Player " + playerName +
              " is starting a raid battle of category " + categoryId);
          }
          var list = playerBattleActor.getPokemonList();
          for (BattlePokemon battlePokemon : list) {
            battlePokemon.setGone(false);
          }
          list.removeIf(battlePokemon -> categoryRaid.isBlackList(battlePokemon.getOriginalPokemon()));
          if (list.isEmpty()) {
            evt.setReason(Text.literal(
              "You don't have any Pokemon that can fight this raid. Please check the blacklist configured."
            ));
            evt.cancel();
            return Unit.INSTANCE;
          }
          return Unit.INSTANCE;
        } else {
          if (CobbleRaids.config.isDebug()) {
            CobbleRaids.LOGGER.info("BATTLE_STARTED_PRE: No categoryId found, normal battle.");
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
          if (pokemonEntity != null) {
            pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
          }
          return Unit.INSTANCE;
        }
        raid.openStartBattleMenu(player);
        return Unit.INSTANCE;
      } catch (Exception e) {
        CobbleRaids.LOGGER.error("Error in BATTLE_STARTED_PRE event: " + e.getMessage());
        e.printStackTrace();
        return Unit.INSTANCE;
      }
    });
  }

  private static void debugBattle(String message, UUID battleId, ServerPlayerEntity player, Iterable<BattleActor> actors,
                                  String categoryId, boolean capture) {
    if (!CobbleRaids.config.isDebug()) return;
    String playerInfo = player == null
      ? "player=null"
      : "player=" + player.getName().getString() + "/" + player.getUuid() + " world=" + player.getWorld().getRegistryKey().getValue();
    CobbleRaids.LOGGER.info(
      CobbleRaids.MOD_ID,
      "BATTLE_STARTED_PRE: " + message +
        " battleId=" + battleId +
        " capture=" + capture +
        " categoryId=" + categoryId +
        " thread=" + Thread.currentThread().getName() +
        " " + playerInfo +
        " actors=" + describeActors(actors)
    );
  }

  private static String describeActors(Iterable<BattleActor> actors) {
    StringBuilder builder = new StringBuilder("[");
    for (BattleActor actor : actors) {
      if (builder.length() > 1) builder.append("; ");
      if (actor instanceof PlayerBattleActor playerActor) {
        builder.append("PlayerBattleActor pokemonCount=").append(playerActor.getPokemonList().size());
        continue;
      }
      if (actor instanceof PokemonBattleActor pokemonActor) {
        builder.append("PokemonBattleActor");
        try {
          var battlePokemon = pokemonActor.getPokemon();
          var entity = battlePokemon.getEntity();
          if (entity == null) {
            builder.append(" entity=null");
          } else {
            var data = entity.getPokemon().getPersistentData();
            builder.append(" entityId=").append(entity.getId())
              .append(" uuid=").append(entity.getUuid())
              .append(" world=").append(entity.getWorld().getRegistryKey().getValue())
              .append(" capture=").append(data.contains(CaptureSessionData.CAPTURE_NBT_KEY))
              .append(" cat=").append(data.getString(Raid.RAID_CATEGORY_NBT_KEY))
              .append(" raid=").append(data.contains(Raid.RAID_NBT_KEY));
          }
        } catch (Exception e) {
          builder.append(" describeError=").append(e.getMessage());
        }
        continue;
      }
      builder.append(actor == null ? "null" : actor.getClass().getSimpleName());
    }
    builder.append("]");
    return builder.toString();
  }
}
