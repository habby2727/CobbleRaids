package com.kingpixel.cobbleraids.config;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.CategoryRaid;
import com.kingpixel.cobbleraids.util.GsonCompat;
import com.kingpixel.cobbleraids.util.ModFiles;
import com.kingpixel.cobbleraids.util.ModRandom;
import com.kingpixel.cobbleutils.util.UtilsFile;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Carlos Varas Alonso - 13/09/2025 2:38
 */
@Data
public class CategoryConfig {
  public static final Path PATH_CATEGORIES = ModFiles.resolve("categories");
  private Map<String, CategoryRaid> categorys = new ConcurrentHashMap<>();

  public void init() {
    categorys.clear();
    var files = ModFiles.files(PATH_CATEGORIES);
    if (files.isEmpty()) {
      createDefaultCategory();
    }
    for (var file : files) {
      try {
        Object parsedCategory = GsonCompat.fromJson(ModFiles.gson(), UtilsFile.readText(file), CategoryRaid.class);
        if (!(parsedCategory instanceof CategoryRaid category)) {
          CobbleRaids.LOGGER.info("Could not deserialize category file: " + file.toAbsolutePath() + ". Skipping...");
          continue;
        }
        if (category.getHealth() <= 0) {
          category.setHealth(100);
        }
        String id = ModFiles.baseName(file);
        category.setId(id);
        category.checker();
        categorys.put(id, category);
        UtilsFile.writeAsync(file, category);
      } catch (Exception e) {
        e.printStackTrace();
        CobbleRaids.LOGGER.info("Could not load category file: " + file.toAbsolutePath() + ". Skipping...");
      }
    }
  }

  private void createDefaultCategory() {
    var defaultCategory = new CategoryRaid("default");
    defaultCategory.checker();
    categorys.put("default", defaultCategory);
    UtilsFile.writeAsync(PATH_CATEGORIES.resolve("default.json"), defaultCategory);
  }

  public CategoryRaid getCategory(String id) {
    return categorys.getOrDefault(id, categorys.get("default"));
  }

  public @Nullable CategoryRaid getRandomCategory() {
    var list = categorys.values().stream().filter(cat -> cat.getChance() > 0).toList();
    double totalChance = list.stream().mapToDouble(CategoryRaid::getChance).sum();
    double randomChance = ModRandom.current().nextDouble() * totalChance;
    double cumulativeChance = 0.0;
    for (CategoryRaid category : list) {
      cumulativeChance += category.getChance();
      if (randomChance <= cumulativeChance) {
        return category;
      }
    }
    return null;
  }
}
