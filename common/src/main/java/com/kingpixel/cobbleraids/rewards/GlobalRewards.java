package com.kingpixel.cobbleraids.rewards;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.AdvancedItemChance;
import lombok.Getter;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 17/01/2025 22:58
 */
@Getter
public class GlobalRewards implements RaidRewards {
  private boolean active;
  private AdvancedItemChance rewards;

  public GlobalRewards() {
    this.active = true;
    this.rewards = new AdvancedItemChance();
  }

  @Override public void giveRewards(Map<UUID, Integer> players) {
    if (active) {
      for (ServerPlayerEntity player : CobbleUtils.server.getPlayerManager().getPlayerList()) {
        rewards.giveRewards(player);
      }
    }
  }
}
