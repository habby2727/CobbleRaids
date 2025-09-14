package com.kingpixel.cobbleraids.events.models;

import com.kingpixel.cobbleraids.models.Raid;
import lombok.Data;

/**
 * @author Carlos Varas Alonso - 13/09/2025 18:51
 */
@Data
public class RaidNewPhase {
  private Raid raid;

  public RaidNewPhase(Raid raid) {
    this.raid = raid;
  }
}
