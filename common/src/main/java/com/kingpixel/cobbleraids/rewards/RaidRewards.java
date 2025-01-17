package com.kingpixel.cobbleraids.rewards;

import java.util.Map;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 17/01/2025 23:22
 */
public interface RaidRewards {
  void giveRewards(Map<UUID, Integer> players);
}
