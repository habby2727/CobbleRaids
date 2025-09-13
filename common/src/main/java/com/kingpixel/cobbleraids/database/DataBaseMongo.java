package com.kingpixel.cobbleraids.database;

import com.kingpixel.cobbleraids.models.UserInfo;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 13/09/2025 17:53
 */
public class DataBaseMongo extends DataBaseClient {

  @Override public void connect() {

  }

  @Override public void disconnect() {

  }

  @Override public UserInfo findUserByPlayer(ServerPlayerEntity player) {
    return null;
  }

  @Override public void saveOrUpdateUserInfo(UserInfo userinfo) {

  }
}
