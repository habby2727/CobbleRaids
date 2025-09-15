package com.kingpixel.cobbleraids.database;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleraids.models.Raid;
import com.kingpixel.cobbleraids.models.UserInfo;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 13/09/2025 17:48
 */
public abstract class DataBaseClient {
  public abstract void connect();

  public abstract void disconnect();

  public static Cache<UUID, UserInfo> cacheUser = Caffeine.newBuilder()
    .expireAfterAccess(5, TimeUnit.MINUTES)
    .build();


  public abstract UserInfo findUserByPlayer(ServerPlayerEntity player);

  public abstract List<Raid> getHistoryRaids(int page, int pageSize);

  public abstract void saveOrUpdateUserInfo(UserInfo userinfo);

  public abstract void saveOrUpdateHistoryRaid(Raid raid);
}
