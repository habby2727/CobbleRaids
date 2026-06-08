package com.kingpixel.cobbleraids.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.CaptureSessionData;
import com.kingpixel.cobbleraids.models.Raid;
import com.kingpixel.cobbleraids.models.rewards.DamageReward;
import com.kingpixel.cobbleraids.util.GsonCompat;
import com.kingpixel.cobbleraids.util.ModFiles;
import com.kingpixel.cobbleutils.Model.AdvancedItemChance;
import com.kingpixel.cobbleutils.util.UtilsFile;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 13/09/2025 18:24ç
 * <p>
 * Manages the rewards configuration for raids, including item chances and other reward-related settings.
 */
public class RewardsManager {
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
    var path = getCaptureRewardsPath();
    var files = ModFiles.files(path);
    if (files.isEmpty()) {
      createDefaultCaptureReward();
      files = ModFiles.files(path);
    }
    for (var file : files) {
      try {
        String content = UtilsFile.readText(file);
        if (content == null || content.isBlank()) {
          continue;
        }
        Object parsedReward = GsonCompat.fromJson(ModFiles.gson(), content, AdvancedItemChance.class);
        if (!(parsedReward instanceof AdvancedItemChance reward)) {
          continue;
        }
        String id = ModFiles.baseName(file);
        CAPTURE_REWARDS.put(id, reward);
        UtilsFile.writeAsync(file, reward);
      } catch (Exception e) {
        CobbleRaids.LOGGER.error("Error loading capture reward file: " + file.toAbsolutePath(), e);
      }
    }
  }

  private void createDefaultCaptureReward() {
    var defaultReward = new AdvancedItemChance();
    UtilsFile.writeAsync(getCaptureRewardsPath().resolve("default.json"), defaultReward);
  }

  private void findDamageRewards() {
    var path = getDamageRewardsPath();
    var files = ModFiles.files(path);
    if (files.isEmpty()) {
      createDamageReward();
      files = ModFiles.files(path);
    }
    for (var file : files) {
      try {
        String content = UtilsFile.readText(file);
        if (content == null || content.isBlank()) {
          continue;
        }
        Object parsedJson = GsonCompat.fromJson(ModFiles.gson(), content, JsonObject.class);
        if (!(parsedJson instanceof JsonObject json)) {
          continue;
        }
        normalizeLegacyRaidBalls(json);
        Object parsedReward = GsonCompat.fromJson(ModFiles.gson(), json, DamageReward.class);
        if (!(parsedReward instanceof DamageReward reward)) {
          continue;
        }
        String id = ModFiles.baseName(file);
        DAMAGE_REWARDS.put(id, reward);
        UtilsFile.writeAsync(file, reward);
      } catch (Exception e) {
        CobbleRaids.LOGGER.error("Error loading damage reward file: " + file.toAbsolutePath(), e);
      }
    }
  }

  private void createDamageReward() {
    var defaultReward = new DamageReward();
    UtilsFile.writeAsync(getDamageRewardsPath().resolve("default.json"), defaultReward);
  }

  private void findVictoryRewards() {
    var path = getVictoryRewardsPath();
    var files = ModFiles.files(path);
    if (files.isEmpty()) {
      createDefaultVictoryReward();
      files = ModFiles.files(path);
    }
    for (var file : files) {
      try {
        String content = UtilsFile.readText(file);
        if (content == null || content.isBlank()) {
          continue;
        }
        Object parsedReward = GsonCompat.fromJson(ModFiles.gson(), content, AdvancedItemChance.class);
        if (!(parsedReward instanceof AdvancedItemChance reward)) {
          continue;
        }
        String id = ModFiles.baseName(file);
        VICTORY_REWARDS.put(id, reward);
        UtilsFile.writeAsync(file, reward);
      } catch (Exception e) {
        CobbleRaids.LOGGER.error("Error loading victory reward file: " + file.toAbsolutePath(), e);
      }
    }
  }

  private void createDefaultVictoryReward() {
    var defaultReward = new AdvancedItemChance();
    UtilsFile.writeAsync(getVictoryRewardsPath().resolve("default.json"), defaultReward);
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
      CobbleRaids.LOGGER.info("No victory reward found for category " + categoryRaid.getId());
    }
    // Damage rewards
    var damageReward = DAMAGE_REWARDS.get(categoryRaid.getId());
    if (damageReward != null) {
      damageReward.giveReward(playerDamageMap, raid);
    } else {
      CobbleRaids.LOGGER.info("No damage reward found for category " + categoryRaid.getId());
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
      CobbleRaids.LOGGER.info("No capture reward found for category " + categoryRaid.getId());
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

  private static Path getRewardsPath() {
    return ModFiles.resolve("rewards");
  }

  private static Path getDamageRewardsPath() {
    return getRewardsPath().resolve("damage_rewards");
  }

  private static Path getVictoryRewardsPath() {
    return getRewardsPath().resolve("victory_rewards");
  }

  private static Path getCaptureRewardsPath() {
    return getRewardsPath().resolve("capture_rewards");
  }
}
