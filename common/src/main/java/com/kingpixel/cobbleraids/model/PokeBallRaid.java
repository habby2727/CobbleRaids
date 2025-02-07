package com.kingpixel.cobbleraids.model;

import com.kingpixel.cobbleutils.Model.ItemModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Carlos Varas Alonso - 06/02/2025 5:12
 */
@EqualsAndHashCode(callSuper = true) @Data
public class PokeBallRaid extends ItemModel {
  private int rateSuccess;

  public PokeBallRaid() {
    super();
    this.rateSuccess = 100;
  }

}
