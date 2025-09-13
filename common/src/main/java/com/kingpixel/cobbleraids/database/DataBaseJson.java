package com.kingpixel.cobbleraids.database;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.UserInfo;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.Utils;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 13/09/2025 17:53
 */
public class DataBaseJson extends DataBaseClient {
  private static final String PATH_DATA = CobbleRaids.PATH + "/data/";

  @Override public void connect() {
    Utils.createDirectoryIfNeeded(PATH_DATA);
    CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "Using JSON database");
  }

  @Override public void disconnect() {
    CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "JSON database disconnected");
  }

  @Override public UserInfo findUserByPlayer(ServerPlayerEntity player) {
    UUID uuid = player.getUuid();
    UserInfo userinfo = cache.get(uuid);
    if (userinfo != null) return userinfo;
    var file = Utils.getAbsolutePath(PATH_DATA + uuid + ".json");
    if (file.exists()) {
      try {
        userinfo = Utils.newGson().fromJson(Utils.readFileSync(file), UserInfo.class);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (userinfo == null) {
      userinfo = new UserInfo(player);
    }
    cache.put(uuid, userinfo);
    Utils.writeFileAsync(file, Utils.newGson().toJson(userinfo));
    return userinfo;
  }


  @Override public void saveOrUpdateUserInfo(UserInfo userinfo) {
    UUID uuid = userinfo.getPlayerUUID();
    cache.put(uuid, userinfo);
    var file = Utils.getAbsolutePath(PATH_DATA + uuid + ".json");
    Utils.writeFileAsync(file, Utils.newGson().toJson(userinfo));
  }
}
