package com.kingpixel.cobbleraids.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.CaptureSessionData;
import com.kingpixel.cobbleraids.models.Raid;
import com.kingpixel.cobbleraids.models.rewards.DamageReward;
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
  private static final Map<String, DamageReward> DAMAGE_REWARDS = new HashMap<>();
  private static final Map<String, AdvancedItemChance> CAPTURE_REWARDS = new HashMap<>();
  // Rewards by states


  public void init() {
    VICTORY_REWARDS.clear();
    DAMAGE_REWARDS.clear();
    CAPTURE_REWARDS.clear();
    findVictoryRewards();
    findDamageRewards();
    findCaptureRewards();
  }

  private void findCaptureRewards() {
    var folder = Utils.getAbsolutePath(PATH_REWARDS + "capture_rewards/");
    var files = Utils.getFiles(folder);
    if (files.isEmpty()) {
      createDefaultCaptureReward();
      files = Utils.getFiles(folder);
    }
    for (var file : files) {
      try {
        var reward = Utils.newGson().fromJson(Utils.readFileSync(file), AdvancedItemChance.class);
        String id = file.getName().replace(".json", "");
        CAPTURE_REWARDS.put(id, reward);
        Utils.writeFileAsync(file, Utils.newGson().toJson(reward));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void createDefaultCaptureReward() {
    var defaultReward = new AdvancedItemChance();
    Utils.writeFileAsync(PATH_REWARDS + "capture_rewards/", "default.json", Utils.newGson().toJson(defaultReward));
  }

  private void findDamageRewards() {
    var folder = Utils.getAbsolutePath(PATH_DAMAGE_REWARDS);
    var files = Utils.getFiles(folder);
    if (files.isEmpty()) {
      createDamageReward();
      files = Utils.getFiles(folder);
    }
    for (var file : files) {
      try {
        var content = Utils.readFileSync(file);
        var json = JsonParser.parseString(content);
        normalizeLegacyRaidBalls(json);
        var reward = Utils.newGson().fromJson(json, DamageReward.class);
        String id = file.getName().replace(".json", "");
        DAMAGE_REWARDS.put(id, reward);
        Utils.writeFileAsync(file, Utils.newGson().toJson(reward));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void createDamageReward() {
    var defaultReward = new DamageReward();
    Utils.writeFileAsync(PATH_DAMAGE_REWARDS, "default.json", Utils.newGson().toJson(defaultReward));
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

  public void giveRewards(Raid raid, Map<UUID, Integer> playerDamageMap) {
    // Victory rewards
    var categoryRaid = raid.getCategoryRaid();
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
    var damageReward = DAMAGE_REWARDS.get(categoryRaid.getId());
    if (damageReward != null) {
      damageReward.giveReward(playerDamageMap, raid);
    } else {
      CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "No damage reward found for category " + categoryRaid.getId());
    }
  }

  public void giveRewardCapture(CaptureSessionData captureSession) {
    var categoryRaid = captureSession.getRaidData().getCategoryRaid();
    var captureReward = CAPTURE_REWARDS.get(categoryRaid.getId());
    if (captureReward != null) {
      var player = captureSession.getPlayer();
      if (player == null) return;
      captureReward.giveRewards(player);
    } else {
      CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "No capture reward found for category " + categoryRaid.getId());
    }

  }

  private void normalizeLegacyRaidBalls(JsonElement json) {
    if (json == null || !json.isJsonObject()) return;
    JsonObject root = json.getAsJsonObject();
    JsonObject rewards = root.getAsJsonObject("rewards");
    if (rewards == null) return;

    for (Map.Entry<String, JsonElement> rewardEntry : rewards.entrySet()) {
      JsonElement rewardValue = rewardEntry.getValue();
      if (rewardValue == null || !rewardValue.isJsonObject()) continue;
      JsonObject rewardObject = rewardValue.getAsJsonObject();
      JsonElement raidBalls = rewardObject.get("raidBalls");
      if (raidBalls == null || raidBalls.isJsonNull()) continue;

      JsonArray normalizedRaidBalls = new JsonArray();
      if (raidBalls.isJsonArray()) {
        for (JsonElement raidBall : raidBalls.getAsJsonArray()) {
          normalizedRaidBalls.add(normalizeLegacyRaidBall(raidBall));
        }
      } else {
        normalizedRaidBalls.add(normalizeLegacyRaidBall(raidBalls));
      }
      rewardObject.add("raidBalls", normalizedRaidBalls);
    }
  }

  private JsonObject normalizeLegacyRaidBall(JsonElement raidBall) {
    if (raidBall != null && raidBall.isJsonObject()) {
      return raidBall.getAsJsonObject();
    }

    JsonObject normalizedRaidBall = new JsonObject();
    String id = "default";
    if (raidBall != null && raidBall.isJsonPrimitive() && raidBall.getAsJsonPrimitive().isString()) {
      id = raidBall.getAsString();
    }
    normalizedRaidBall.addProperty("id", id);
    return normalizedRaidBall;
  }
}
