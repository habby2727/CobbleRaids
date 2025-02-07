package com.kingpixel.cobbleraids.config;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.model.Raid;
import com.kingpixel.cobbleraids.model.TypeRaid;
import com.kingpixel.cobbleraids.rewards.DamageRewards;
import com.kingpixel.cobbleraids.rewards.GlobalRewards;
import com.kingpixel.cobbleraids.rewards.LastHitRewards;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Carlos Varas Alonso - 17/01/2025 0:26
 */
@Getter
public class RaidsConfig {
  public static Map<TypeRaid, List<Raid>> raids = new HashMap<>();

  public static Raid getRandomRaid(TypeRaid typeRaid) {
    var list = raids.get(typeRaid);
    double totalWeight = list.stream().mapToDouble(Raid::getChance).sum();
    double random = Math.random() * totalWeight;
    for (Raid raid : list) {
      random -= raid.getChance();
      if (random <= 0) {
        return raid;
      }
    }
    return list.getLast();
  }


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
          Raid raid;
          try {
            raid = Utils.newGson().fromJson(Utils.readFileSync(file), Raid.class);
            if (raid == null) {
              CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "Error reading raid " + file.getAbsolutePath());
              continue;
            }
            raid.setId(file.getName().replace(".json", ""));
            raid.check();
            Utils.writeFileAsync(CobbleRaids.PATH_RAIDS, raid.getId() + ".json", Utils.newGson().toJson(raid));
            raid.setRewards(new ArrayList<>());
            addDamageReward(raid);
            addGlobalReward(raid);
            addKillReward(raid);
            raids.computeIfAbsent(raid.getType(), k -> new ArrayList<>()).add(raid);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
  }

  private void addDamageReward(Raid raid) throws IOException {
    File file = Utils.getAbsolutePath(CobbleRaids.PATH_RAID_DAMAGE_REWARDS + raid.getId() + ".json");
    DamageRewards damageRewards;
    if (file.exists()) {
      damageRewards = Utils.newGson().fromJson(Utils.readFileSync(file), DamageRewards.class);
      raid.getRewards().add(damageRewards);
    } else {
      damageRewards = new DamageRewards();
      raid.getRewards().add(damageRewards);
      CobbleUtils.LOGGER.warn(CobbleRaids.MOD_ID, "No damage rewards found for raid " + raid.getId() + ". Creating default.");
    }
    damageRewards.check();
    Utils.writeFileAsync(CobbleRaids.PATH_RAID_DAMAGE_REWARDS, raid.getId() + ".json", Utils.newGson().toJson(damageRewards));
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


  private void addKillReward(Raid raid) throws IOException {
    File file = Utils.getAbsolutePath(CobbleRaids.PATH_RAID_KILL_REWARDS + raid.getId() + ".json");
    if (file.exists()) {
      LastHitRewards killRewards = Utils.newGson().fromJson(Utils.readFileSync(file), LastHitRewards.class);
      raid.getRewards().add(killRewards);
    } else {
      LastHitRewards killRewards = new LastHitRewards();
      raid.getRewards().add(killRewards);
      CobbleUtils.LOGGER.warn(CobbleRaids.MOD_ID, "No kill rewards found for raid " + raid.getId() + ". Creating default.");
      Utils.writeFileAsync(CobbleRaids.PATH_RAID_KILL_REWARDS, raid.getId() + ".json", Utils.newGson().toJson(killRewards));
    }
  }

  private void createDefaultRaids() {
    raids.put(TypeRaid.GLOBAL, new ArrayList<>());
    Raid raid = new Raid();
    raid.setId("default");
    raid.setName("&cdefault");
    raids.forEach((type, list) -> {
      list.add(raid);
      try {
        Utils.writeFileAsync(CobbleRaids.PATH_RAIDS, raid.getId() + ".json", Utils.newGson().toJson(raid));
        addDamageReward(raid);
        addGlobalReward(raid);
        addKillReward(raid);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  public Raid getRaid(String id) {
    return raids.values().stream().flatMap(List::stream).filter(tag -> tag.getId().equals(id)).findFirst().orElse(null);
  }


}
