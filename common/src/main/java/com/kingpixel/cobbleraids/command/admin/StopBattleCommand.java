package com.kingpixel.cobbleraids.command.admin;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

/**
 * @author Carlos Varas Alonso - 13/09/2025 4:16
 */
public class StopBattleCommand {
  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    base.then(
      CommandManager.literal("stopBattle")
        .executes(context -> {
          var player = context.getSource().getPlayer();
          if (player == null) return 0;
          var fightData = CobbleRaids.raidManager.getFightingPlayer(player.getUuid());
          if (fightData == null) return 1;
          fightData.stop(true);
          return 1;
        })
        .then(
          CommandManager.argument("player", EntityArgumentType.players())
            .requires(source -> PermissionApi.hasPermission(
              source,
              CobbleRaids.MOD_ID + ".admin",
              2
            ))
            .executes(context -> {
              var players = EntityArgumentType.getPlayers(context, "player");
              for (var player : players) {
                var fightData = CobbleRaids.raidManager.getFightingPlayer(player.getUuid());
                if (fightData != null) {
                  fightData.stop(true);
                }
              }
              return 1;
            })
        )
    );
  }
}
