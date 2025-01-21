package com.kingpixel.cobbleraids.rewards;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.Model.AdvancedItemChance;
import lombok.Getter;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 17/01/2025 22:58
 */
@Getter
public class KillRewards extends RaidRewards {
  private final AdvancedItemChance reward;


  public KillRewards() {
    super();
    this.reward = new AdvancedItemChance();
    reward.setTitle("Global Reward");
  }

  public void giveRewards(ServerPlayerEntity player) {
    if (active) {
      if (player == null) return;
      reward.giveRewards(player);
    }
  }

  @Override public void open(ServerPlayerEntity player) {
    reward.openMenu(player, template -> {
    }, close -> {
      CobbleRaids.language.getMenuRewards().open(player);
    });
  }
}
