package com.kingpixel.cobbleraids.manager;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.FightData;
import com.kingpixel.cobbleraids.models.Raid;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

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

  public Raid getRaid(UUID raidUUID) {
    return activeRaids.get(raidUUID);
  }

  public void generateRaid(UUID raidUUID, Raid raid) {
    activeRaids.put(raidUUID, raid);
  }

  public void removeRaid(UUID raidUUID) {
    if (raidUUID == null) return;
    activeRaids.remove(raidUUID);
    if (randomRaidUUID == null) return;
    if (randomRaidUUID.equals(raidUUID)) {
      randomRaidOn = false;
      randomRaidUUID = null;
    }
  }

  public void addFightingData(UUID battleUUID, FightData fightData) {
    fightingByBattleUUID.put(battleUUID, fightData);
    fightingByPlayers.put(fightData.getPlayer().getUuid(), fightData);
  }

  public FightData removeFightingData(UUID battleUUID) {
    var fight = fightingByBattleUUID.remove(battleUUID);
    if (fight != null) fightingByPlayers.remove(fight.getPlayer().getUuid());
    return fight;
  }

  public @Nullable FightData getFightingData(UUID battleUUID) {
    if (battleUUID == null) return null;
    return fightingByBattleUUID.get(battleUUID);
  }

  public List<FightData> getFightingsByRaidUUID(UUID raidUUID) {
    return fightingByBattleUUID.values().stream().filter(fightData -> fightData.getRaid().getRaidUUID().equals(raidUUID)).toList();
  }

  public void stopAllRaids() {
    for (Map.Entry<UUID, Raid> entry : activeRaids.entrySet()) {
      var raid = entry.getValue();
      if (raid == null) continue;
      raid.finishRaid();
    }
    activeRaids.clear();
    for (Map.Entry<UUID, FightData> entry : fightingByBattleUUID.entrySet()) {
      var fightData = entry.getValue();
      if (fightData == null) continue;
      fightData.stop(true);
    }
  }

  public void stopRaidPlayer(ServerPlayerEntity player) {
    var fight = fightingByPlayers.get(player.getUuid());
    if (fight != null) fight.stop(true);

  }

  public FightData getFightingPlayer(UUID playerUUID) {
    if (!fightingByPlayers.containsKey(playerUUID)) return null;
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
