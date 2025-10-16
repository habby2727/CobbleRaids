package com.kingpixel.cobbleraids.events.raids.models;

import com.kingpixel.cobbleraids.models.Raid;
import lombok.Data;

/**
 * @author Carlos Varas Alonso - 13/09/2025 18:51
 */
@Data
public class RaidFinished {
  private Raid raid;
  private boolean killed;

  public RaidFinished(Raid raid, boolean killed) {
    this.raid = raid;
    this.killed = killed;
  }
}
