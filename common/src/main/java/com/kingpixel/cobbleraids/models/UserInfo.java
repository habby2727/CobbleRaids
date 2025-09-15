package com.kingpixel.cobbleraids.models;

import com.kingpixel.cobbleraids.database.DataBaseFactory;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 13/09/2025 2:34
 */
@Data
public class UserInfo {
  private UUID playerUUID;
  private String playerName;
  private Map<String, Integer> tickets;
  private Map<String, Long> bans;

  public UserInfo(ServerPlayerEntity player) {
    this.playerUUID = player.getUuid();
    this.playerName = player.getGameProfile().getName();
    this.tickets = new HashMap<>();
  }

  public boolean hasTicket(CategoryRaid categoryRaid) {
    return tickets.getOrDefault(categoryRaid.getId(), 0) > 0;
  }

  public void removeTicket(CategoryRaid categoryRaid) {
    if (!categoryRaid.isNeedTicket()) return;
    tickets.put(categoryRaid.getId(), Math.max(0, tickets.getOrDefault(categoryRaid.getId(), 0) - 1));
    DataBaseFactory.INSTANCE.saveOrUpdateUserInfo(this);
  }

  public void addTickets(String categoryId, int amount) {
    tickets.put(categoryId, tickets.getOrDefault(categoryId, 0) + amount);
    DataBaseFactory.INSTANCE.saveOrUpdateUserInfo(this);
  }

  public boolean isBanned(CategoryRaid categoryRaid) {
    if (bans == null) return false;
    long finishAt = bans.getOrDefault(categoryRaid.getId(), 0L);
    if (finishAt == 0L) return false;
    if (System.currentTimeMillis() > finishAt) {
      bans.remove(categoryRaid.getId());
      DataBaseFactory.INSTANCE.saveOrUpdateUserInfo(this);
      return false;
    }
    return true;
  }

  public void banCategory(String categoryId, long durationMs) {
    if (bans == null) bans = new HashMap<>();
    bans.put(categoryId, System.currentTimeMillis() + durationMs);
  }
}
