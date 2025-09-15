package com.kingpixel.cobbleraids.database;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.Raid;
import com.kingpixel.cobbleraids.models.UserInfo;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.Utils;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
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
    UserInfo userinfo = cacheUser.getIfPresent(uuid);
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
    cacheUser.put(uuid, userinfo);
    Utils.writeFileAsync(file, Utils.newGson().toJson(userinfo));
    return userinfo;
  }

  @Override public List<Raid> getHistoryRaids(int page, int pageSize) {
    return List.of();
  }


  @Override public void saveOrUpdateUserInfo(UserInfo userinfo) {
    UUID uuid = userinfo.getPlayerUUID();
    cacheUser.put(uuid, userinfo);
    var file = Utils.getAbsolutePath(PATH_DATA + uuid + ".json");
    Utils.writeFileAsync(file, Utils.newGson().toJson(userinfo));
  }

  @Override public void saveOrUpdateHistoryRaid(Raid raid) {

  }
}
