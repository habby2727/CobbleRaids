package com.kingpixel.cobbleraids.events;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.model.Raid;
import com.kingpixel.cobbleutils.CobbleUtils;
import kotlin.Unit;

/**
 * @author Carlos Varas Alonso - 08/02/2025 19:37
 */
public class ExpEvents {
  public static void register() {
    CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE.subscribe(Priority.NORMAL, (evt) -> {
      if (Raid.isRaid(evt.getPokemon())) {
        if (CobbleRaids.config.isDebug()) {
          CobbleUtils.LOGGER.info("CobbleRaids - Blocked XP gain for " + evt.getPokemon().showdownId());
        }
        evt.setExperience(0);
      }
      return Unit.INSTANCE;
    });
  }
}
