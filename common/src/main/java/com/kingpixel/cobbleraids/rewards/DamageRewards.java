package com.kingpixel.cobbleraids.rewards;

import com.kingpixel.cobbleutils.Model.AdvancedItemChance;
import lombok.Getter;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Improved by GitHub Copilot
 */
@Getter
public class DamageRewards extends RaidRewards {
  private boolean active;
  private Map<String, AdvancedItemChance> rewards;

  public DamageRewards() {
    this.active = true;
    this.rewards = new HashMap<>();
    // Example rewards setup
    rewards.put("1=", new AdvancedItemChance());
    rewards.put("2=", new AdvancedItemChance());
    rewards.put("3=", new AdvancedItemChance());
    rewards.put("4>=", new AdvancedItemChance());
  }

  @Override
  public void giveRewards(Map<UUID, Integer> players) {
    if (!active) return;
    if (players.isEmpty()) return;
    // Sort players by damage in descending order
    List<Map.Entry<UUID, Integer>> sortedPlayers = players.entrySet().stream()
      .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
      .toList();

    for (Map.Entry<String, AdvancedItemChance> rewardEntry : rewards.entrySet()) {
      String condition = rewardEntry.getKey();
      AdvancedItemChance reward = rewardEntry.getValue();

      // Parse the condition
      Pattern pattern = Pattern.compile("(\\d+)([<=>]+)");
      Matcher matcher = pattern.matcher(condition);
      if (matcher.matches()) {
        int topN = Integer.parseInt(matcher.group(1));
        String operator = matcher.group(2);

        // Apply the condition
        switch (operator) {
          case "<=":
            for (int i = 0; i < Math.min(topN, sortedPlayers.size()); i++) {
              UUID playerUUID = sortedPlayers.get(i).getKey();
              giveRewardToPlayer(playerUUID, reward);
            }
            break;
          case ">=":
            for (int i = topN - 1; i < sortedPlayers.size(); i++) {
              UUID playerUUID = sortedPlayers.get(i).getKey();
              giveRewardToPlayer(playerUUID, reward);
            }
            break;
          case "=":
            if (topN - 1 < sortedPlayers.size()) {
              UUID playerUUID = sortedPlayers.get(topN - 1).getKey();
              giveRewardToPlayer(playerUUID, reward);
            }
            break;
        }
      }
    }
  }

  @Override public void open(ServerPlayerEntity player) {

  }

}