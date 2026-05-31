package com.kingpixel.cobbleraids.command.admin;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.database.DataBaseFactory;
import com.kingpixel.cobbleraids.models.UserInfo;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.command.suggests.CobbleUtilsSuggests;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 13/09/2025 4:16
 */
public class RaidBanCommand {
  private static final List<String> PERMISSION = List.of(
    CobbleRaids.MOD_ID + ".admin"
  );

  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    base.then(
      CommandManager.literal("ban")
        .requires(source -> PermissionApi.hasPermission(
          source,
          CobbleRaids.MOD_ID + ".admin",
          2
        ))
        .then(
          CommandManager.argument("categoryId", StringArgumentType.string())
            .suggests((context, builder) -> {
              CobbleRaids.categorys.getCategorys().forEach((k, v) -> {
                builder.suggest(k);
              });
              return builder.buildFuture();
            }).then(
              CobbleUtilsSuggests.SUGGESTS_PLAYER_OFFLINE_AND_ONLINE.suggestPlayerName("player", PERMISSION, 2)
                .then(
                  CommandManager.argument("time", StringArgumentType.string())
                    .suggests(DurationValue.INSTANCE::listSuggestions)
                    .executes(context -> {
                      String categoryId = StringArgumentType.getString(context, "categoryId");
                      String playerName = StringArgumentType.getString(context, "player");
                      String timeStr = StringArgumentType.getString(context, "time");
                      long durationMs = DurationValue.parse(timeStr).toMillis();
                      CobbleUtilsSuggests.SUGGESTS_PLAYER_OFFLINE_AND_ONLINE.getPlayer(playerName).ifPresent(result -> {
                        UserInfo userInfo = DataBaseFactory.INSTANCE.findUserByPlayer(result.player());
                        if (userInfo == null) return;
                        userInfo.banCategory(categoryId, durationMs);
                        DataBaseFactory.INSTANCE.saveOrUpdateUserInfo(userInfo);
                      });
                      return 1;
                    })
                )
            )
        )
    );
  }
}
