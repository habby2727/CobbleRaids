package com.kingpixel.cobbleraids.events;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.model.CaptureSession;
import com.kingpixel.cobbleraids.model.Raid;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.common.InteractionEvent;
import kotlin.Unit;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 18/01/2025 22:06
 */
public class CaptureEvents {
  public static void register() {
    InteractionEvent.RIGHT_CLICK_ITEM.register((playerEntity, hand) -> {
      var player = (ServerPlayerEntity) playerEntity;
      ItemStack itemStack = player.getStackInHand(hand);
      if (itemStack.isEmpty()) return CompoundEventResult.pass();
      var battle = Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player);
      if (battle == null) return CompoundEventResult.pass();
      for (BattleActor actor : battle.getActors()) {
        if (actor instanceof PokemonBattleActor pokemonBattleActor) {
          var pokemonEntity = pokemonBattleActor.getEntity();
          if (pokemonEntity == null) continue;
          var pokemon = pokemonEntity.getPokemon();
          if (pokemon.getPersistentData().getBoolean(CobbleRaids.TAG_RAID_CAPTURE)) {
            String id = itemStack.getItem().toString();
            if (CobbleRaids.config.isDebug()) CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "Item: " + id);
            if (CobbleRaids.config.isNeedPokeBallRaids() && !CobbleRaids.config.getPokeballs().contains(id)) {
              PlayerUtils.sendMessage(
                player,
                CobbleRaids.language.getMessageEnabledPokeBalls()
                  .replace("%pokeballs%", String.join(", ", CobbleRaids.config.getPokeballs())),
                CobbleRaids.config.getPrefix(),
                TypeMessage.CHAT
              );
              return CompoundEventResult.interrupt(true, itemStack);
            }
          }
        }
      }
      return CompoundEventResult.pass();
    });
    // Battle events
    CobblemonEvents.BATTLE_FLED.subscribe(Priority.LOWEST, (evt) -> {
      ServerPlayerEntity player = null;
      PokemonEntity pokemonEntity = null;
      boolean isRaidCapture = false;
      for (BattleActor actor : evt.getBattle().getActors()) {
        if (actor instanceof PokemonBattleActor pokemonBattleActor) {
          var pokemonBattleActorEntity = pokemonBattleActor.getEntity();
          if (pokemonBattleActorEntity == null) continue;
          var pokemon = pokemonBattleActorEntity.getPokemon();
          if (pokemon.getPersistentData().getBoolean(CobbleRaids.TAG_RAID_CAPTURE) && pokemonEntity == null) {
            pokemonEntity = pokemonBattleActorEntity;
            isRaidCapture = true;
          }
        } else if (actor instanceof PlayerBattleActor playerBattleActor) {
          player = playerBattleActor.getEntity();
        }
      }
      if (isRaidCapture && player != null) {
        var battle = Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player);
        if (battle != null) battle.end();
        CaptureSession.removeUuid(pokemonEntity.getPokemon().getUuid());
        pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
        return Unit.INSTANCE;
      }
      return Unit.INSTANCE;
    });

    CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.LOWEST, (evt) -> {
      var battle = evt.getBattle();
      for (BattleActor actor : battle.getActors()) {
        if (actor instanceof PokemonBattleActor pokemonBattleActor) {
          var pokemonEntity = pokemonBattleActor.getEntity();
          if (pokemonEntity == null) continue;
          var pokemon = pokemonEntity.getPokemon();
          if (pokemon.getPersistentData().getBoolean(CobbleRaids.TAG_RAID_CAPTURE)) {
            CaptureSession.removeUuid(pokemonEntity.getPokemon().getUuid());
            pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
            return Unit.INSTANCE;
          }
        }
      }
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
          if (CobbleRaids.config.isNeedPokeBallRaids() && !CobbleRaids.config.getPokeballs().contains(id)) {
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


}
