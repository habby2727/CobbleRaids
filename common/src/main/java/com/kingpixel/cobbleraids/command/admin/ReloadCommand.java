package com.kingpixel.cobbleraids.command.admin;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 13/09/2025 4:15
 */
public class ReloadCommand {
  private static final List<String> PERMISSION = List.of("cobbleraids.admin.reload");

  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    base.then(
      CommandManager.literal("reload")
        .requires(source -> PermissionApi.hasPermission(source, PERMISSION, 2))
        .executes(context -> {
          CobbleRaids.load();
          context.getSource().sendMessage(
            AdventureTranslator.toNative(
              CobbleRaids.language.getReload(),
              CobbleRaids.language.getPrefix()
            )
          );
          return 1;
        }));
  }
}
