package com.kingpixel.cobbleraids.command;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.managers.BattleManager;
import com.kingpixel.cobbleraids.model.PokemonRaid;
import com.kingpixel.cobbleraids.model.Raid;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.joml.Vector3d;

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
          .executes(context -> {
            if (context.getSource().isExecutedByPlayer()) {
              ServerPlayerEntity player = context.getSource().getPlayer();
              if (CobbleRaids.battleManager != null) {
                Raid raid = CobbleRaids.battleManager.getRaid();
                Vector3d pos = raid.getPosPlayer();
                player.teleport(PokemonRaid.getWorld(raid), pos.x(), pos.y(), pos.z(), player.getYaw(),
                  player.getPitch());
              }
            }
            return 1;
          })
          .then(
            CommandManager.literal("reload")
              .requires(source ->
                PermissionApi.hasPermission(source, List.of(CobbleRaids.MOD_ID + ".admin"), 2))
              .executes(context -> {
                CobbleRaids.load();
                if (context.getSource().isExecutedByPlayer()) {
                  PlayerUtils.sendMessage(
                    context.getSource().getPlayer(),
                    CobbleRaids.language.getMessageReload(),
                    CobbleRaids.config.getPrefix(),
                    TypeMessage.CHAT
                  );
                } else {
                  CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, CobbleRaids.language.getMessageReload()
                    .replace("%prefix%", CobbleRaids.config.getPrefix()));
                }
                return 1;
              })
          )
          .then(
            CommandManager.literal("start")
              .requires(source ->
                PermissionApi.hasPermission(source, List.of(CobbleRaids.MOD_ID + ".admin"), 2))
              .then(
                CommandManager.argument("raid", StringArgumentType.string())
                  .suggests((context, builder) -> {
                    CobbleRaids.raidsConfig.getRaids().forEach(raid -> {
                      builder.suggest(raid.getId());
                    });
                    return builder.buildFuture();
                  })
                  .executes(context -> {
                    Raid raid = CobbleRaids.raidsConfig.getRaid(StringArgumentType.getString(context,
                      "raid"));
                    BattleManager.startRaid(raid);
                    return 1;
                  })
              )
          ).then(
            CommandManager.literal("finish")
              .requires(source ->
                PermissionApi.hasPermission(source, List.of(CobbleRaids.MOD_ID + ".admin"), 2))
              .executes(context -> {
                if (CobbleRaids.battleManager != null) {
                  CobbleRaids.battleManager.finishRaid();
                }
                return 1;
              })
          ).then(
            CommandManager.literal("tp")
              .requires(source ->
                PermissionApi.hasPermission(source, List.of(CobbleRaids.MOD_ID + ".admin"), 2))
              .then(
                CommandManager.argument("raid", StringArgumentType.string())
                  .executes(context -> {
                    if (!context.getSource().isExecutedByPlayer()) return 0;
                    Raid raid = CobbleRaids.raidsConfig.getRaid(StringArgumentType.getString(context,
                      "raid"));
                    Vector3d pos = raid.getPosPlayer();
                    if (context.getSource().isExecutedByPlayer()) {
                      ServerPlayerEntity player = context.getSource().getPlayer();
                      player.teleport(PokemonRaid.getWorld(raid), pos.x(), pos.y(), pos.z(), player.getYaw(),
                        player.getPitch());
                    }
                    return 1;
                  })
              )
          )
      );

    });
  }


}
