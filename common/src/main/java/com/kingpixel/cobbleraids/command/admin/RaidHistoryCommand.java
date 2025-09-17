package com.kingpixel.cobbleraids.command.admin;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.gui.HistoryMenu;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

/**
 * @author Carlos Varas Alonso - 13/09/2025 4:16
 */
public class RaidHistoryCommand {
  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    base.then(
      CommandManager.literal("history")
        .requires(source -> PermissionApi.hasPermission(
          source,
          CobbleRaids.MOD_ID + ".admin",
          2
        ))
        .executes(context -> {
          if (!context.getSource().isExecutedByPlayer()) return 0;
          var player = context.getSource().getPlayer();
          if (player == null) return 0;
          HistoryMenu.open(player, 1);
          return 1;
        })
    );
  }
}
