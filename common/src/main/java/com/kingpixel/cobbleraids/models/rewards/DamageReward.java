package com.kingpixel.cobbleraids.models.rewards;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.Raid;
import com.kingpixel.cobbleraids.models.RaidBall;
import com.kingpixel.cobbleutils.Model.AdvancedItemChance;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * @author Carlos
 */
@Data
public class DamageReward {
  private Map<String, DamageRewardData> rewards = new HashMap<>();

  public DamageReward() {
    rewards.put("1", new DamageRewardData());
    rewards.put("2", new DamageRewardData());
    rewards.put("3", new DamageRewardData());
    rewards.put("4-100", new DamageRewardData());
  }

  /**
   * Devuelve el reward correspondiente a una posición (place)
   */
  private DamageRewardData getRewardForPlace(int place) {
    for (Map.Entry<String, DamageRewardData> entry : rewards.entrySet()) {
      String key = entry.getKey();

      if (key.contains("-")) { // es un rango
        String[] parts = key.split("-");
        int min = Integer.parseInt(parts[0]);
        int max = Integer.parseInt(parts[1]);

        if (place >= min && place <= max) {
          return entry.getValue();
        }
      } else { // es número exacto
        int pos = Integer.parseInt(key);
        if (pos == place) {
          return entry.getValue();
        }
      }
    }
    return null; // si no hay reward para esa posición
  }

  /**
   * Dar recompensas en base al daño (ejemplo simplificado)
   */
  public void giveReward(Map<UUID, Integer> damageMap, Raid raid) {
    // Ordenamos por mayor daño (posición = ranking)
    List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(damageMap.entrySet());
    sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

    int place = 1;
    for (Map.Entry<UUID, Integer> entry : sorted) {
      UUID playerId = entry.getKey();

      DamageRewardData reward = getRewardForPlace(place);
      if (reward == null) continue;
      ServerPlayerEntity player = CobbleRaids.server.getPlayerManager().getPlayer(playerId);
      if (player == null) continue;
      reward.giveRewards(player, raid);
      place++;
    }
  }

  @Data
  private static class DamageRewardData {
    private boolean activeCaptureSession;
    private int chance;
    private AdvancedItemChance reward;
    private List<RaidBall> raidBalls;

    public DamageRewardData() {
      this.activeCaptureSession = true;
      this.chance = 1;
      this.reward = new AdvancedItemChance();
      this.raidBalls = new ArrayList<>();
      this.raidBalls.add(new RaidBall());
    }

    public void giveRewards(ServerPlayerEntity player, Raid raid) {
      if (reward != null) {
        reward.giveRewards(player);
      }
      if (raidBalls != null && !raidBalls.isEmpty()) {
        for (RaidBall raidBall : raidBalls) {
          raidBall.giveToPlayer(player, 1);
        }
      }
      startSessionCapture(player, raid);
    }

    public void startSessionCapture(ServerPlayerEntity player, Raid raid) {
      // Lógica para iniciar la sesión de captura
      boolean isOkey = Utils.getRandom().nextInt(chance) == 0;
      if (isOkey && activeCaptureSession) {
        raid.getPlayersInitCaptureSession().add(player.getUuid());
        // raid.getCaptureSessionManager().startSession(player, raid);
        CobbleRaids.captureSessionManager.startSession(
          player,
          raid
        );
      } else {
        CobbleRaids.language.getMessageNotLuckyCapture().sendMessage(
          player, CobbleRaids.language.getPrefix(), false
        );
      }
    }
  }
}
