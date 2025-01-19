package com.kingpixel.cobbleraids.rewards;

import com.kingpixel.cobbleraids.CobbleRaids;
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
public class GlobalRewards extends RaidRewards {
  private boolean active;
  private RewardGiven rewardGiven;
  private AdvancedItemChance reward;

  enum RewardGiven {
    PARTICIPANTS,
    ALL
  }


  public GlobalRewards() {
    this.active = true;
    this.rewardGiven = RewardGiven.ALL;
    this.reward = new AdvancedItemChance();
    reward.setTitle("Global Reward");
  }

  @Override public void giveRewards(Map<UUID, Integer> players) {
    if (active) {
      if (players.isEmpty()) return;
      if (rewardGiven == null) return;
      if (rewardGiven == RewardGiven.PARTICIPANTS) {
        for (Map.Entry<UUID, Integer> playerEntry : players.entrySet()) {
          giveRewardToPlayer(playerEntry.getKey(), reward);
        }
      } else {
        for (ServerPlayerEntity player : CobbleUtils.server.getPlayerManager().getPlayerList()) {
          reward.giveRewards(player);
        }
      }
    }
  }

  @Override public void open(ServerPlayerEntity player) {
    reward.openMenu(player, template -> {
    }, close -> {
      CobbleRaids.language.getMenuRewards().open(player);
    });
  }
}
