package com.kingpixel.cobbleraids.command.base;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.Raid;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 13/09/2025 4:15
 */
public class RaidJoinCommand {
  private static final List<String> PERMISSION = List.of("cobbleraids.user");

  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    base.then(
      CommandManager.literal("join")
        .executes(context -> {
          if (!context.getSource().isExecutedByPlayer()) return 0;
          ServerPlayerEntity player = context.getSource().getPlayer();
          if (player == null) return 0;
          if (PlayerUtils.isBattle(player)) return 0;
          var entities = player.getWorld().getEntitiesByClass(PokemonEntity.class,
            player.getBoundingBox().expand(32),
            e -> e.getPokemon().getPersistentData().contains(Raid.RAID_NBT_KEY));
          if (entities.isEmpty()) {
            PlayerUtils.sendMessage(
              player,
              "&cNo raid found nearby!",
              CobbleRaids.language.getPrefix(),
              TypeMessage.CHAT
            );
            return 1;
          }
          var first = entities.getFirst();
          UUID raidUUID = first.getPokemon().getPersistentData().getUuid(Raid.RAID_NBT_KEY);
          Raid raid = CobbleRaids.raidManager.getRaid(raidUUID);
          if (raid == null) {
            PlayerUtils.sendMessage(
              player,
              "&cNo raid found nearby!",
              CobbleRaids.language.getPrefix(),
              TypeMessage.CHAT
            );
            first.discard();
            return 1;
          }
          raid.openStartBattleMenu(player);
          return 1;
        }));
  }
}
