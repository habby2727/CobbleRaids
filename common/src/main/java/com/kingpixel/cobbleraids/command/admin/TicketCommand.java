package com.kingpixel.cobbleraids.command.admin;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.database.DataBaseFactory;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 13/09/2025 4:15
 */
public class TicketCommand {
  private static final List<String> PERMISSION = List.of("cobbleraids.admin.reload");

  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    base.then(
      CommandManager.literal("ticket")
        .then(
          CommandManager.argument("category", StringArgumentType.string())
            .suggests((context, builder) -> {
              CobbleRaids.categorys.getCategorys().forEach((s, categoryRaid) -> builder.suggest(s));
              return builder.buildFuture();
            })
            .then(
              CommandManager.argument("amount", IntegerArgumentType.integer(1))
                .then(
                  CommandManager.argument("player", EntityArgumentType.players())
                    .executes(context -> {
                      String categoryId = StringArgumentType.getString(context, "category");
                      var category = CobbleRaids.categorys.getCategory(categoryId);
                      if (category == null) {
                        context.getSource().sendMessage(
                          AdventureTranslator.toNative(
                            "&cCategory not found!",
                            CobbleRaids.language.getPrefix()
                          )
                        );
                        return 0;
                      }
                      int amount = IntegerArgumentType.getInteger(context, "amount");
                      var players = EntityArgumentType.getPlayers(context, "player");
                      String message = CobbleRaids.language.getMessageReceivedTicket()
                        .replace("%category%", categoryId)
                        .replace("%amount%", String.valueOf(amount));

                      for (var player : players) {
                        var userinfo = DataBaseFactory.INSTANCE.findUserByPlayer(player);
                        userinfo.addTickets(categoryId, amount);
                        Text result = AdventureTranslator.toNative(
                          message
                            .replace("%total%", String.valueOf(userinfo.getTickets().getOrDefault(categoryId, 0))),
                          CobbleRaids.language.getPrefix()
                        );
                        player.sendMessage(result);
                      }
                      return 1;
                    })
                )
            )
        )
    );
  }
}
