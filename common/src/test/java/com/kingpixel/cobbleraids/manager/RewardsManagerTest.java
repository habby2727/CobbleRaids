package com.kingpixel.cobbleraids.manager;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.kingpixel.cobbleraids.models.rewards.DamageReward;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RewardsManagerTest {
  private static final Gson GSON = new Gson();

  @Test
  void normalizesLegacyRaidBallStringArraysBeforeDamageRewardDeserialization() throws Exception {
    String legacyJson = """
      {
        "rewards": {
          "1": {
            "activeCaptureSession": true,
            "sessionRate": 1,
            "reward": {
              "id": "",
              "showMenu": true,
              "title": "Top daño #1",
              "giveAll": true,
              "amountRewardsPermission": {
                "": 1
              },
              "lootTable": {
                "": []
              }
            },
            "raidBalls": ["jefe_breloom", "jefe_charmeleon"]
          },
          "2": {
            "activeCaptureSession": true,
            "sessionRate": 1,
            "reward": {
              "id": "",
              "showMenu": true,
              "title": "Top daño #2",
              "giveAll": true,
              "amountRewardsPermission": {
                "": 1
              },
              "lootTable": {
                "": []
              }
            },
            "raidBalls": [{
              "id": "default",
              "catchChance": 42.5,
              "amount": 8,
              "item": "cobblemon:great_ball"
            }]
          }
        }
      }
      """;

    JsonElement json = JsonParser.parseString(legacyJson);
    RewardsManager rewardsManager = new RewardsManager();
    Method normalizeMethod = RewardsManager.class.getDeclaredMethod("normalizeLegacyRaidBalls", JsonElement.class);
    normalizeMethod.setAccessible(true);

    assertDoesNotThrow(() -> normalizeMethod.invoke(rewardsManager, json));
    assertThat(json.getAsJsonObject()
      .getAsJsonObject("rewards")
      .getAsJsonObject("1")
      .getAsJsonArray("raidBalls"))
      .allSatisfy(raidBall -> assertThat(raidBall.isJsonObject()).isTrue());

    DamageReward reward = assertDoesNotThrow(() -> GSON.fromJson(json, DamageReward.class));
    assertThat(reward).isNotNull();
    assertThat(assertDoesNotThrow(() -> GSON.toJson(reward))).isNotBlank();
  }
}
