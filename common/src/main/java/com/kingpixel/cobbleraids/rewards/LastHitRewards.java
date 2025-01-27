package com.kingpixel.cobbleraids.rewards;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.Model.AdvancedItemChance;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import lombok.Getter;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 17/01/2025 22:58
 */
@Getter
public class LastHitRewards extends RaidRewards {
  private final AdvancedItemChance reward;


  public LastHitRewards() {
    super();
    this.reward = new AdvancedItemChance();
    reward.setTitle("Last Hit Reward");
  }

  public void giveRewards(ServerPlayerEntity player) {
    if (active) {
      if (player == null) return;
      PlayerUtils.sendMessage(
        player,
        CobbleRaids.language.getMessageLastHit()
          .replace("%player%", player.getGameProfile().getName()),
        CobbleRaids.config.getPrefix(),
        TypeMessage.CHAT
      );
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
