package com.kingpixel.cobbleraids.command;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 10/06/2024 14:08
 */
public class CommandTree {

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registry) {
    CobbleRaids.config.getCommands().forEach(command -> {
      LiteralArgumentBuilder<ServerCommandSource> base = CommandManager.literal(command)
        .requires(source ->
          PermissionApi.hasPermission(source, List.of(CobbleRaids.MOD_ID + ".admin", CobbleRaids.MOD_ID + ".user"), 2));
      dispatcher.register(
        base
      );

    });
  }


}
