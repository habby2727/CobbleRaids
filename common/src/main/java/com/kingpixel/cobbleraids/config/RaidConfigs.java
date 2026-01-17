package com.kingpixel.cobbleraids.config;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.CategoryRaid;
import com.kingpixel.cobbleraids.models.RaidData;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Carlos Varas Alonso - 13/09/2025 2:28
 */
@Data
public class RaidConfigs {
  private static final String PATH_RAIDS = CobbleRaids.PATH + "/raids/";
  private Map<String, RaidData> raids = new ConcurrentHashMap<>();
  private Map<String, List<RaidData>> raidsByCategory = new ConcurrentHashMap<>();

  public void init() {
    raids.clear();
    raidsByCategory.clear();
    var files = Utils.getFiles(Utils.getAbsolutePath(PATH_RAIDS));
    if (files.isEmpty()) {
      createDefaultRaid();
      return;
    }
    for (var file : files) {
      try {
        var raid = Utils.newGson().fromJson(Utils.readFileSync(file), RaidData.class);
        String id = file.getName().replace(".json", "");
        raid.setId(id);
        raids.put(id, raid);
        CategoryRaid category = raid.getCategoryRaid();
        if (category == null) {
          CobbleUtils.LOGGER.warn("Raid " + id + " has an invalid category.");
          continue;
        } else {
          raidsByCategory.computeIfAbsent(category.getId(), k -> new ArrayList<>()).add(raid);
        }
        Utils.writeFileAsync(file, Utils.newGson().toJson(raid));
      } catch (Exception e) {
        e.printStackTrace();
        CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "Could not load raid file: " + file.getAbsolutePath() + ". Skipping...");
      }
    }
  }

  public RaidData getRaid(String id) {
    return raids.getOrDefault(id, raids.get("default"));
  }

  public List<RaidData> getRaidsByCategory(String categoryId) {
    return raidsByCategory.getOrDefault(categoryId, raidsByCategory.get("default"));
  }

  public RaidData getRandomRaidByCategory(String categoryId) {
    var raids = getRaidsByCategory(categoryId);
    if (raids.isEmpty()) return this.raids.get("default");
    double totalchance = raids.stream().mapToDouble(RaidData::getChance).sum();
    double random = Utils.getRandom().nextDouble() * totalchance;
    double cumulativeChance = 0.0;
    for (var raid : raids) {
      cumulativeChance += raid.getChance();
      if (random <= cumulativeChance) {
        return raid;
      }
    }
    return raids.getFirst();
  }

  private void createDefaultRaid() {
    var defaultRaid = new RaidData("default", "default");
    raids.put("default", defaultRaid);
    Utils.writeFileAsync(PATH_RAIDS, "default.json", Utils.newGson().toJson(defaultRaid));
  }
}
