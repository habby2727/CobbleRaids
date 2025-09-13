package com.kingpixel.cobbleraids.config;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.CategoryRaid;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.AdvancedItemChance;
import com.kingpixel.cobbleutils.util.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 13/09/2025 18:24ç
 * <p>
 * Manages the rewards configuration for raids, including item chances and other reward-related settings.
 */
public class RewardsManager {
  private static final String PATH_REWARDS = CobbleRaids.PATH + "/rewards/";
  private static final String PATH_DAMAGE_REWARDS = PATH_REWARDS + "damage_rewards/";
  private static final String PATH_VICTORY_REWARDS = PATH_REWARDS + "victory_rewards/";
  private static final Map<String, AdvancedItemChance> VICTORY_REWARDS = new HashMap<>();

  public void init() {
    VICTORY_REWARDS.clear();
    findVictoryRewards();
  }

  private void findVictoryRewards() {
    var folder = Utils.getAbsolutePath(PATH_VICTORY_REWARDS);
    var files = Utils.getFiles(folder);
    if (files.isEmpty()) {
      createDefaultVictoryReward();
      files = Utils.getFiles(folder);
    }
    for (var file : files) {
      try {
        var reward = Utils.newGson().fromJson(Utils.readFileSync(file), AdvancedItemChance.class);
        String id = file.getName().replace(".json", "");
        VICTORY_REWARDS.put(id, reward);
        Utils.writeFileAsync(file, Utils.newGson().toJson(reward));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void createDefaultVictoryReward() {
    var defaultReward = new AdvancedItemChance();
    Utils.writeFileAsync(PATH_VICTORY_REWARDS, "default.json", Utils.newGson().toJson(defaultReward));
  }

  public void giveRewards(CategoryRaid categoryRaid, Map<UUID, Integer> playerDamageMap) {
    // Victory rewards
    var victoryReward = VICTORY_REWARDS.get(categoryRaid.getId());
    if (victoryReward != null) {
      playerDamageMap.forEach((uuid, integer) -> {
        var player = CobbleRaids.server.getPlayerManager().getPlayer(uuid);
        if (player == null) return;
        victoryReward.giveRewards(player);
      });
    } else {
      CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "No victory reward found for category " + categoryRaid.getId());
    }
    // Damage rewards
    
  }
}
