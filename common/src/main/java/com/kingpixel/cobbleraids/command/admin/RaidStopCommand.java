package com.kingpixel.cobbleraids.command.admin;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

/**
 * @author Carlos Varas Alonso - 13/09/2025 4:16
 */
public class RaidStopCommand {
  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    base.then(
      CommandManager.literal("stopAll")
        .requires(source -> PermissionApi.hasPermission(
          source,
          CobbleRaids.MOD_ID + ".admin",
          2
        ))
        .executes(context -> {
          try {
            CobbleRaids.captureSessionManager.finishAllSessions();
            CobbleRaids.raidManager.stopAllRaids();
          } catch (Exception e) {
            e.printStackTrace();
          }
          return 1;
        })
    );
  }
}
