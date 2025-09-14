package com.kingpixel.cobbleraids.manager;

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
  // Map<raidUUID, Raid>
  private final Map<UUID, Raid> activeRaids = new ConcurrentHashMap<>();
  // Map<BattleUUID, FightData>
  private final Map<UUID, FightData> fightingPlayers = new ConcurrentHashMap<>();

  public Raid getRaid(UUID raidUUID) {
    return activeRaids.get(raidUUID);
  }

  public void generateRaid(UUID raidUUID, Raid raid) {
    activeRaids.put(raidUUID, raid);
  }

  public Raid removeRaid(UUID raidUUID) {
    return activeRaids.remove(raidUUID);
  }

  public void addFightingData(UUID BattleUUID, FightData fightData) {
    fightingPlayers.put(BattleUUID, fightData);
  }

  public void removeFightingData(UUID BattleUUID) {
    fightingPlayers.remove(BattleUUID);
  }

  public FightData getFightingData(UUID BattleUUID) {
    return fightingPlayers.get(BattleUUID);
  }

  public List<FightData> getFightingPlayers(UUID raidUUID) {
    return fightingPlayers.values().stream().filter(fightData -> fightData.getRaid().getRaidUUID().equals(raidUUID)).toList();
  }

  public void stopAllRaids() {
    activeRaids.forEach((uuid, raid) -> {
      raid.getRaidEntity().discard();
    });
    activeRaids.clear();
    fightingPlayers.forEach((uuid, fightData) -> {
      fightData.stop();
    });
    fightingPlayers.clear();
  }

  public void stopRaidPlayer(ServerPlayerEntity player) {
    fightingPlayers.values().stream().filter(fight -> fight.getPlayer().getUuid().equals(player.getUuid())).findFirst().ifPresent(FightData::stop);
  }

  public FightData getFightingPlayer(UUID uuid) {
    return fightingPlayers.values().stream().filter(fight -> fight.getPlayer().getUuid().equals(uuid)).findFirst().orElse(null);
  }
}
