package com.kingpixel.cobbleraids.manager;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.FightData;
import com.kingpixel.cobbleraids.models.Raid;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Carlos Varas Alonso - 13/09/2025 4:10
 */

@Data
public class RaidManager {
  private boolean randomRaidOn;
  private UUID randomRaidUUID;
  // Map<raidUUID, Raid>
  private final Map<UUID, Raid> activeRaids = new ConcurrentHashMap<>();
  // Map<BattleUUID, FightData>
  private final Map<UUID, FightData> fightingByBattleUUID = new ConcurrentHashMap<>();
  private final Map<UUID, FightData> fightingByPlayers = new ConcurrentHashMap<>();
  private final Map<UUID, FightData> fightingByRaidUUID = new ConcurrentHashMap<>();

  public Raid getRaid(UUID raidUUID) {
    return activeRaids.get(raidUUID);
  }

  public void generateRaid(UUID raidUUID, Raid raid) {
    activeRaids.put(raidUUID, raid);
  }

  public void removeRaid(UUID raidUUID) {
    activeRaids.remove(raidUUID);
    if (randomRaidUUID.equals(raidUUID)) {
      randomRaidOn = false;
      randomRaidUUID = null;
    }
  }

  public void addFightingData(UUID BattleUUID, FightData fightData) {
    fightingByBattleUUID.put(BattleUUID, fightData);
    fightingByPlayers.put(fightData.getPlayer().getUuid(), fightData);
    fightingByRaidUUID.put(fightData.getRaid().getRaidUUID(), fightData);
  }

  public void removeFightingData(UUID BattleUUID) {
    var fight = fightingByBattleUUID.remove(BattleUUID);
    if (fight != null) {
      fightingByPlayers.remove(fight.getPlayer().getUuid());
      fightingByRaidUUID.remove(fight.getRaid().getRaidUUID());
    }
  }

  public FightData getFightingData(UUID BattleUUID) {
    return fightingByBattleUUID.get(BattleUUID);
  }

  public List<FightData> getFightingsByRaidUUID(UUID raidUUID) {
    return fightingByBattleUUID.values().stream().filter(fightData -> fightData.getRaid().getRaidUUID().equals(raidUUID)).toList();
  }

  public void stopAllRaids() {
    activeRaids.forEach((uuid, raid) -> {
      raid.finishRaid();
      if (raid.getRaidEntity() == null) return;
      raid.getRaidEntity().discard();
    });
    activeRaids.clear();
    fightingByBattleUUID.forEach((uuid, fightData) -> {
      fightData.stop();
    });
    fightingByBattleUUID.clear();
  }

  public void stopRaidPlayer(ServerPlayerEntity player) {
    var fight = fightingByPlayers.get(player.getUuid());
    if (fight != null) {
      fight.stop();
    }
  }

  public FightData getFightingPlayer(UUID playerUUID) {
    return fightingByPlayers.get(playerUUID);
  }

  public void initRandomRaid() {
    this.randomRaidOn = true;
    var category = CobbleRaids.categorys.getRandomCategory();
    if (category == null) {
      this.randomRaidOn = false;
      this.randomRaidUUID = null;
      return;
    }
    var raidData = category.getRandomRaid();
    if (raidData == null) {
      this.randomRaidOn = false;
      this.randomRaidUUID = null;
      return;
    }
    var raid = new Raid(raidData, CobbleRaids.config.getCooldownBetweenRaids().toMillis());
    randomRaidUUID = raid.getRaidUUID();
    CobbleRaids.raidManager.generateRaid(raid.getRaidUUID(), raid);
  }
}
