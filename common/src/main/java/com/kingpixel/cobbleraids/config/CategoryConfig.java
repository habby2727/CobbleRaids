package com.kingpixel.cobbleraids.config;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.CategoryRaid;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Carlos Varas Alonso - 13/09/2025 2:38
 */
@Data
public class CategoryConfig {
  public static final String PATH_CATEGORIES = CobbleRaids.PATH + "/categories/";
  private final Map<String, CategoryRaid> categorys = new HashMap<>();

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
        categorys.put(id, category);
        Utils.writeFileAsync(file, Utils.newGson().toJson(category));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void createDefaultCategory() {
    var defaultCategory = new CategoryRaid("default");
    categorys.put("default", defaultCategory);
    Utils.writeFileAsync(PATH_CATEGORIES, "default.json", Utils.newGson().toJson(defaultCategory));
  }

  public CategoryRaid getCategory(String id) {
    return categorys.getOrDefault(id, categorys.get("default"));
  }
}
