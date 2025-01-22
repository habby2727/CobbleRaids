package com.kingpixel.cobbleraids.rewards;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.AdvancedItemChance;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import lombok.Getter;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 17/01/2025 22:58
 */
@Getter
public class GlobalRewards extends RaidRewards {
  private final RewardGiven rewardGiven;
  private final AdvancedItemChance reward;

  enum RewardGiven {
    PARTICIPANTS,
    ALL
  }


  public GlobalRewards() {
    super();
    this.active = false;
    this.rewardGiven = RewardGiven.PARTICIPANTS;
    this.reward = new AdvancedItemChance();
    reward.setTitle("Global Reward");
  }

  @Override public void giveRewards(Map<UUID, Integer> players) {
    if (active) {
      players.forEach((key, value) -> {
        ServerPlayerEntity player = CobbleUtils.server.getPlayerManager().getPlayer(key);
        if (player == null) return;
        PlayerUtils.sendMessage(
          player,
          CobbleRaids.language.getMessageRewardGlobal(),
          CobbleRaids.config.getPrefix(),
          TypeMessage.CHAT
        );
      });
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
