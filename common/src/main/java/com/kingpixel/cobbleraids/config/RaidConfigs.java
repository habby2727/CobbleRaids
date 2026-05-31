package com.kingpixel.cobbleraids.config;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.CategoryRaid;
import com.kingpixel.cobbleraids.models.RaidData;
import com.kingpixel.cobbleraids.util.GsonCompat;
import com.kingpixel.cobbleraids.util.ModFiles;
import com.kingpixel.cobbleraids.util.ModRandom;
import com.kingpixel.cobbleutils.util.UtilsFile;
import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Carlos Varas Alonso - 13/09/2025 2:28
 */
@Data
public class RaidConfigs {
  private static final Path PATH_RAIDS = ModFiles.resolve("raids");
  private Map<String, RaidData> raids = new ConcurrentHashMap<>();
  private Map<String, List<RaidData>> raidsByCategory = new ConcurrentHashMap<>();

  public void init() {
    raids.clear();
    raidsByCategory.clear();
    var files = ModFiles.files(PATH_RAIDS);
    if (files.isEmpty()) {
      createDefaultRaid();
      return;
    }
    for (var file : files) {
      try {
        Object parsedRaid = GsonCompat.fromJson(ModFiles.gson(), UtilsFile.readText(file), RaidData.class);
        if (!(parsedRaid instanceof RaidData raid)) {
          CobbleRaids.LOGGER.warn("Raid file " + file.toAbsolutePath() + " could not be deserialized.");
          continue;
        }
        String id = ModFiles.baseName(file);
        raid.setId(id);
        raids.put(id, raid);
        CategoryRaid category = raid.getCategoryRaid();
        if (category == null) {
          CobbleRaids.LOGGER.warn("Raid " + id + " has an invalid category.");
          continue;
        } else {
          raidsByCategory.computeIfAbsent(category.getId(), k -> new ArrayList<>()).add(raid);
        }
        UtilsFile.writeAsync(file, raid);
      } catch (Exception e) {
        e.printStackTrace();
        CobbleRaids.LOGGER.info("Could not load raid file: " + file.toAbsolutePath() + ". Skipping...");
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
    double random = ModRandom.current().nextDouble() * totalchance;
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
    UtilsFile.writeAsync(PATH_RAIDS.resolve("default.json"), defaultRaid);
  }
}
