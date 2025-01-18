package com.kingpixel.cobbleraids.config;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.model.Raid;
import com.kingpixel.cobbleraids.rewards.DamageRewards;
import com.kingpixel.cobbleraids.rewards.GlobalRewards;
import com.kingpixel.cobbleraids.rewards.RaidRewards;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 17/01/2025 0:26
 */
@Getter
public class RaidsConfig {
  public List<Raid> raids = new ArrayList<>();


  public void init() {
    raids.clear();
    File folder = Utils.getAbsolutePath(CobbleRaids.PATH_RAIDS);

    if (!folder.exists()) {
      folder.mkdirs();
      createDefaultRaids();
    } else {
      File[] files = folder.listFiles();
      if (files != null) {
        for (File file : files) {
          Raid raid = null;
          try {
            raid = Utils.newGson().fromJson(Utils.readFileSync(file), Raid.class);
            raid.setId(file.getName().replace(".json", ""));
            raid.check();
            Utils.writeFileAsync(CobbleRaids.PATH_RAIDS, raid.getId() + ".json", Utils.newGson().toJson(raid));
            raid.setRewards(new ArrayList<>());
            addDamageReward(raid);
            addGlobalReward(raid);
            raids.add(raid);
            for (RaidRewards reward : raid.getRewards()) {
              if (CobbleUtils.config.isDebug()) {
                CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID,
                  "Loaded rewards for raid " + raid.getId() + ": " + reward.toString());
              }
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
  }

  private void addDamageReward(Raid raid) throws IOException {
    File file = Utils.getAbsolutePath(CobbleRaids.PATH_RAID_DAMAGE_REWARDS + raid.getId() + ".json");
    if (file.exists()) {
      DamageRewards damageRewards = Utils.newGson().fromJson(Utils.readFileSync(file), DamageRewards.class);
      raid.getRewards().add(damageRewards);
    } else {
      DamageRewards damageRewards = new DamageRewards();
      raid.getRewards().add(damageRewards);
      CobbleUtils.LOGGER.warn(CobbleRaids.MOD_ID, "No damage rewards found for raid " + raid.getId() + ". Creating default.");
      Utils.writeFileAsync(CobbleRaids.PATH_RAID_DAMAGE_REWARDS, raid.getId() + ".json", Utils.newGson().toJson(damageRewards));
    }
  }

  private void addGlobalReward(Raid raid) throws IOException {
    File file = Utils.getAbsolutePath(CobbleRaids.PATH_RAID_GLOBAL_REWARDS + raid.getId() + ".json");
    if (file.exists()) {
      GlobalRewards globalRewards = Utils.newGson().fromJson(Utils.readFileSync(file), GlobalRewards.class);
      raid.getRewards().add(globalRewards);
    } else {
      GlobalRewards globalRewards = new GlobalRewards();
      raid.getRewards().add(globalRewards);
      CobbleUtils.LOGGER.warn(CobbleRaids.MOD_ID, "No global rewards found for raid " + raid.getId() + ". Creating default.");
      Utils.writeFileAsync(CobbleRaids.PATH_RAID_GLOBAL_REWARDS, raid.getId() + ".json", Utils.newGson().toJson(globalRewards));
    }
  }


  private void createDefaultRaids() {
    raids.add(new Raid());
    raids.forEach(tag -> {
      Utils.writeFileAsync(CobbleRaids.PATH_RAIDS, tag.getId() + ".json", Utils.newGson().toJson(tag));
    });
  }

  public Raid getRaid(String id) {
    return raids.stream().filter(tag -> tag.getId().equals(id)).findFirst().orElse(null);
  }

  public Raid getRandomRaid() {
    double totalWeight = raids.stream().mapToDouble(Raid::getChance).sum();
    double random = Math.random() * totalWeight;
    for (Raid raid : raids) {
      random -= raid.getChance();
      if (random <= 0) {
        return raid;
      }
    }
    return null;
  }


}
