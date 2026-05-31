package com.kingpixel.cobbleraids.command.admin;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.Raid;
import com.kingpixel.cobbleraids.models.RaidData;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * @author Carlos Varas Alonso - 13/09/2025 4:16
 */
public class RaidStartCommand {
  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    base.then(
      CommandManager.literal("start")
        .requires(source -> PermissionApi.hasPermission(
          source,
          CobbleRaids.MOD_ID + ".admin",
          2
        ))
        .then(
          CommandManager.argument("category", StringArgumentType.string())
            .suggests((context, builder) -> {
              CobbleRaids.categorys.getCategorys().forEach((s, categoryRaid) -> builder.suggest(s));
              return builder.buildFuture();
            }).then(
              CommandManager.argument("raidId", StringArgumentType.string())
                .suggests((context, builder) -> {
                  String categoryId = StringArgumentType.getString(context, "category");
                  var list = CobbleRaids.raidConfigs.getRaidsByCategory(categoryId);
                  if (list == null || list.isEmpty()) return builder.buildFuture();
                  for (RaidData raidData : list) {
                    builder.suggest(raidData.getId());
                  }
                  return builder.buildFuture();
                })
                .executes(context -> {
                  String raidId = StringArgumentType.getString(context, "raidId");
                  try {
                    RaidData raidData = CobbleRaids.raidConfigs.getRaid(raidId);
                    if (raidData == null) {
                      context.getSource().sendError(Text.literal("Raid not found: " + raidId));
                      return 0;
                    }
                    Raid raid = new Raid(raidData);
                    CobbleRaids.raidManager.generateRaid(raid.getRaidUUID(), raid);
                    context.getSource().sendFeedback(() -> Text.literal("Raid started: " + raidId), false);
                  } catch (Exception e) {
                    context.getSource().sendError(Text.literal("Could not start raid " + raidId + ": " + e.getMessage()));
                    e.printStackTrace();
                    return 0;
                  }
                  return 1;
                }).then(
                  CommandManager.argument("time", StringArgumentType.string())
                    .executes(context -> {
                      String raidId = StringArgumentType.getString(context, "raidId");
                      try {
                        String time = StringArgumentType.getString(context, "time");
                        long millis = DurationValue.parse(time).toMillis();
                        RaidData raidData = CobbleRaids.raidConfigs.getRaid(raidId);
                        if (raidData == null) {
                          context.getSource().sendError(Text.literal("Raid not found: " + raidId));
                          return 0;
                        }
                        Raid raid = new Raid(raidData, millis);
                        CobbleRaids.raidManager.generateRaid(raid.getRaidUUID(), raid);
                        context.getSource().sendFeedback(() -> Text.literal("Raid scheduled: " + raidId + " in " + time), false);
                      } catch (Exception e) {
                        context.getSource().sendError(Text.literal("Could not schedule raid " + raidId + ": " + e.getMessage()));
                        e.printStackTrace();
                        return 0;
                      }
                      return 1;
                    })
                )
            )
        )
    );
  }
}
