package com.kingpixel.cobbleraids.command;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.command.admin.*;
import com.kingpixel.cobbleraids.command.base.RaidJoinCommand;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

/**
 * @author Carlos Varas Alonso - 13/09/2025 2:03
 */
public class CommandTree {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registry) {
    for (String literal : CobbleRaids.config.getCommands()) {
      var base = CommandManager.literal(literal);

      ReloadCommand.register(base);
      RaidStartCommand.register(base);
      RaidStopCommand.register(base);
      RaidJoinCommand.register(base);
      RaidHistoryCommand.register(base);
      TicketCommand.register(base);
      StopBattleCommand.register(base);
      RaidBanCommand.register(base);

      dispatcher.register(base);
    }
  }
}
