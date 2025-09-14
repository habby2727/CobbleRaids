package com.kingpixel.cobbleraids.config;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.RaidData;
import com.kingpixel.cobbleutils.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Carlos Varas Alonso - 13/09/2025 2:28
 */
public class RaidConfigs {
  private static final String PATH_RAIDS = CobbleRaids.PATH + "/raids/";
  public static final Map<String, RaidData> RAIDS = new HashMap<>();
  public static final Map<String, List<RaidData>> RAIDS_BY_CATEGORY = new HashMap<>();

  public void init() {
    RAIDS.clear();
    RAIDS_BY_CATEGORY.clear();
    var files = Utils.getFiles(Utils.getAbsolutePath(PATH_RAIDS));
    if (files.isEmpty()) {
      createDefaultRaid();
      return;
    }
    for (var file : files) {
      try {
        var raid = Utils.newGson().fromJson(Utils.readFileSync(file), RaidData.class);
        String id = file.getName().replace(".json", "");
        RAIDS.put(id, raid);
        RAIDS_BY_CATEGORY.computeIfAbsent(raid.getCategoryRaid().getId(), k -> new ArrayList<>()).add(raid);
        Utils.writeFileAsync(file, Utils.newGson().toJson(raid));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public RaidData getRaid(String id) {
    return RAIDS.getOrDefault(id, RAIDS.get("default"));
  }

  public List<RaidData> getRaidsByCategory(String categoryId) {
    return RAIDS_BY_CATEGORY.getOrDefault(categoryId, RAIDS_BY_CATEGORY.get("default"));
  }

  public RaidData getRandomRaidByCategory(String categoryId) {
    var raids = getRaidsByCategory(categoryId);
    if (raids.isEmpty()) return RAIDS.get("default");
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
    RAIDS.put("default", defaultRaid);
    Utils.writeFileAsync(PATH_RAIDS, "default.json", Utils.newGson().toJson(defaultRaid));
  }
}
