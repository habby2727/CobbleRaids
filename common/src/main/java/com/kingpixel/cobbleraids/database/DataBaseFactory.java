package com.kingpixel.cobbleraids.database;

import com.kingpixel.cobbleraids.CobbleRaids;

/**
 * @author Carlos Varas Alonso - 13/09/2025 17:50
 */
public class DataBaseFactory {
  public static DataBaseClient INSTANCE;

  public static void init() {
    if (INSTANCE != null) INSTANCE.disconnect();
    var db = CobbleRaids.config.getDatabase();
    INSTANCE = switch (db.getType()) {
      case MONGODB -> new DataBaseMongo();
      case JSON -> new DataBaseJson();
      default -> throw new IllegalStateException("Unexpected value: " + db.getType());
    };
    INSTANCE.connect();
  }
}
