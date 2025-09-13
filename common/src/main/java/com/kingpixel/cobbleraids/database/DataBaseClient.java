package com.kingpixel.cobbleraids.database;

import com.kingpixel.cobbleraids.models.UserInfo;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 13/09/2025 17:48
 */
public abstract class DataBaseClient {
  public abstract void connect();

  public abstract void disconnect();

  public static Map<UUID, UserInfo> cache = new HashMap<>();

  public abstract UserInfo findUserByPlayer(ServerPlayerEntity player);

  public abstract void saveOrUpdateUserInfo(UserInfo userinfo);
}
