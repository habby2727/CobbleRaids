package com.kingpixel.cobbleraids.command;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.config.PokeBallRaidConfig;
import com.kingpixel.cobbleraids.config.RaidsConfig;
import com.kingpixel.cobbleraids.managers.BattleManager;
import com.kingpixel.cobbleraids.model.*;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.joml.Vector3d;

import java.util.List;
import java.util.UUID;

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
            for (RaidStarted activeRaid : BattleManager.activeRaids) {
              if (activeRaid == null) continue;
              if (activeRaid.getRaid().getType().equals(TypeRaid.GLOBAL)) {
                ServerPlayerEntity player = context.getSource().getPlayer();
                if (player == null) return 0;
                player.teleport(PokemonRaid.getWorld(activeRaid.getRaid()), activeRaid.getRaid().getPosPlayer().x(),
                  activeRaid.getRaid().getPosPlayer().y(), activeRaid.getRaid().getPosPlayer().z(), player.getYaw(),
                  player.getPitch());
                return 0;
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
                    for (List<Raid> value : RaidsConfig.raids.values()) {
                      for (Raid raid : value) {
                        builder.suggest(raid.getId());
                      }
                    }
                    return builder.buildFuture();
                  })
                  .executes(context -> {
                    Raid raid = CobbleRaids.raidsConfig.getRaid(StringArgumentType.getString(context,
                      "raid"));
                    if (raid == null) {
                      context.getSource().sendMessage(Text.literal("Raid not found"));
                      return 0;
                    }
                    BattleManager.startRaid(raid, null);
                    return 1;
                  })
              )
          ).then(
            CommandManager.literal("finish")
              .requires(source ->
                PermissionApi.hasPermission(source, List.of(CobbleRaids.MOD_ID + ".admin"), 2))
              .then(
                CommandManager.argument("raid", UuidArgumentType.uuid())
                  .suggests((context, builder) -> {
                    BattleManager.activeRaids.forEach(raid -> {
                      builder.suggest(raid.getRaidUUID().toString());
                    });
                    return builder.buildFuture();
                  })
                  .executes(context -> {
                      RaidStarted raidStarted = BattleManager.getActiveRaid(UuidArgumentType.getUuid(context, "raid"));
                      raidStarted.finish();
                      return 1;
                    }
                  )
              )
          ).then(
            CommandManager.literal("time")
              .requires(source ->
                PermissionApi.hasPermission(source, List.of(CobbleRaids.MOD_ID + ".admin", CobbleRaids.MOD_ID +
                  ".user"), 2))
              .executes(context -> {
                if (context.getSource().isExecutedByPlayer()) {
                  ServerPlayerEntity player = context.getSource().getPlayer();
                  PlayerUtils.sendMessage(player, "Raid start in " + PlayerUtils.getCooldown(CobbleRaids.startDate),
                    CobbleRaids.config.getPrefix(), TypeMessage.CHAT);
                }
                return 1;
              })
          )
          .then(
            CommandManager.literal("tp")
              .requires(source ->
                PermissionApi.hasPermission(source, List.of(CobbleRaids.MOD_ID + ".admin"), 2))
              .then(
                CommandManager.argument("raid", StringArgumentType.string())
                  .suggests((context, builder) -> {
                    BattleManager.activeRaids.forEach(raidStarted -> {
                      builder.suggest(raidStarted.getRaidUUID().toString());
                    });
                    return builder.buildFuture();
                  })
                  .executes(context -> {
                    if (!context.getSource().isExecutedByPlayer()) return 0;
                    String s = StringArgumentType.getString(context,
                      "raid");
                    RaidStarted raid = BattleManager.getActiveRaid(UUID.fromString(s));
                    Vector3d pos = raid.getRaid().getPosPlayer();
                    if (context.getSource().isExecutedByPlayer()) {
                      ServerPlayerEntity player = context.getSource().getPlayer();
                      if (player == null) return 0;
                      player.teleport(PokemonRaid.getWorld(raid.getRaid()), pos.x(), pos.y(), pos.z(), player.getYaw(),
                        player.getPitch());
                    }
                    return 1;
                  })
              )
          ).then(
            CommandManager.literal("pokeball")
              .requires(source ->
                PermissionApi.hasPermission(source, List.of(CobbleRaids.MOD_ID + ".admin"), 2))
              .then(
                CommandManager.argument("pokeballId", StringArgumentType.string())
                  .suggests((context, builder) -> {
                    for (PokeBallRaid pokeBallRaid : PokeBallRaidConfig.pokeBallRaids) {
                      builder.suggest(pokeBallRaid.getId());
                    }
                    return builder.buildFuture();
                  })
                  .then(
                    CommandManager.argument("player", EntityArgumentType.player())
                      .executes(context -> {
                        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                        String id = StringArgumentType.getString(context, "pokeballId");
                        if (player == null) return 0;
                        PokeBallRaidConfig.give(player, id, 1);
                        return 1;
                      })
                      .then(
                        CommandManager.argument("amount", IntegerArgumentType.integer())
                          .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            String id = StringArgumentType.getString(context, "pokeballId");
                            int amount = IntegerArgumentType.getInteger(context, "amount");
                            if (player == null) return 0;
                            PokeBallRaidConfig.give(player, id, amount);
                            return 1;
                          })
                      )
                  )

              )
          )
      );
    });
  }


}
