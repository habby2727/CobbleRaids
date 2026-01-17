package com.kingpixel.cobbleraids.config;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.CategoryRaid;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Carlos Varas Alonso - 13/09/2025 2:38
 */
@Data
public class CategoryConfig {
  public static final String PATH_CATEGORIES = CobbleRaids.PATH + "/categories/";
  private Map<String, CategoryRaid> categorys = new ConcurrentHashMap<>();

  public void init() {
    categorys.clear();
    var files = Utils.getFiles(Utils.getAbsolutePath(PATH_CATEGORIES));
    if (files.isEmpty()) {
      createDefaultCategory();
    }
    for (var file : files) {
      try {
        var category = Utils.newGson().fromJson(Utils.readFileSync(file), CategoryRaid.class);
        if (category.getHealth() <= 0) {
          category.setHealth(100);
        }
        String id = file.getName().replace(".json", "");
        category.setId(id);
        category.checker();
        categorys.put(id, category);
        Utils.writeFileAsync(file, Utils.newGson().toJson(category));
      } catch (Exception e) {
        e.printStackTrace();
        CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "Could not load category file: " + file.getAbsolutePath() + ". Skipping...");
      }
    }
  }

  private void createDefaultCategory() {
    var defaultCategory = new CategoryRaid("default");
    defaultCategory.checker();
    categorys.put("default", defaultCategory);
    Utils.writeFileAsync(PATH_CATEGORIES, "default.json", Utils.newGson().toJson(defaultCategory));
  }

  public CategoryRaid getCategory(String id) {
    return categorys.getOrDefault(id, categorys.get("default"));
  }

  public @Nullable CategoryRaid getRandomCategory() {
    var list = categorys.values().stream().filter(cat -> cat.getChance() > 0).toList();
    var random = Utils.getRandom();
    double totalChance = list.stream().mapToDouble(CategoryRaid::getChance).sum();
    double randomChance = random.nextDouble() * totalChance;
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
