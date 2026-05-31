package com.kingpixel.cobbleraids.database;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.Raid;
import com.kingpixel.cobbleraids.models.UserInfo;
import com.kingpixel.cobbleraids.util.GsonCompat;
import com.kingpixel.cobbleraids.util.ModFiles;
import com.kingpixel.cobbleutils.util.UtilsFile;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 13/09/2025 17:53
 */
public class DataBaseJson extends DataBaseClient {
  private static final Path PATH_DATA = ModFiles.resolve("data");

  @Override public void connect() {
    try {
      ModFiles.ensureDirectory(PATH_DATA);
    } catch (IOException e) {
      throw new IllegalStateException("Could not create JSON database directory.", e);
    }
    CobbleRaids.LOGGER.info("Using JSON database");
  }

  @Override public void disconnect() {
    CobbleRaids.LOGGER.info("JSON database disconnected");
  }

  @Override public UserInfo findUserByPlayer(ServerPlayerEntity player) {
    UUID uuid = player.getUuid();
    UserInfo userinfo = cacheUser.getIfPresent(uuid);
    if (userinfo != null) return userinfo;
    var file = PATH_DATA.resolve(uuid + ".json");
    if (UtilsFile.exists(file)) {
      try {
        Object parsedUser = GsonCompat.fromJson(ModFiles.gson(), UtilsFile.readText(file), UserInfo.class);
        if (parsedUser instanceof UserInfo user) {
          userinfo = user;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (userinfo == null) {
      userinfo = new UserInfo(player);
    }
    cacheUser.put(uuid, userinfo);
    UtilsFile.writeAsync(file, userinfo);
    return userinfo;
  }

  @Override public List<Raid> getHistoryRaids(int page, int pageSize) {
    return List.of();
  }


  @Override public void saveOrUpdateUserInfo(UserInfo userinfo) {
    UUID uuid = userinfo.getPlayerUUID();
    cacheUser.put(uuid, userinfo);
    var file = PATH_DATA.resolve(uuid + ".json");
    UtilsFile.writeAsync(file, userinfo);
  }

  @Override public void saveOrUpdateHistoryRaid(Raid raid) {

  }
}
