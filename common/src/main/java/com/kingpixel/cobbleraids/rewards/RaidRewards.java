package com.kingpixel.cobbleraids.rewards;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.Model.AdvancedItemChance;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 17/01/2025 23:22
 */
public abstract class RaidRewards {


  public void giveRewards(Map<UUID, Integer> players) {

  }

  void open(ServerPlayerEntity player) {

  }

  void giveRewardToPlayer(UUID playerUUID, AdvancedItemChance reward) {
    ServerPlayerEntity player = CobbleRaids.server.getPlayerManager().getPlayer(playerUUID);
    if (player == null) return;
    reward.giveRewards(player);
  }
}
