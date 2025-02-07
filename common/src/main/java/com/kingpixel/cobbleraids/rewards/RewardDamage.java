package com.kingpixel.cobbleraids.rewards;

import com.kingpixel.cobbleutils.Model.AdvancedItemChance;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Carlos Varas Alonso - 25/01/2025 5:21
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Data
public class RewardDamage extends AdvancedItemChance {
  private boolean captureFight;
  private int rateSuccess;

  public RewardDamage() {
    super();
    this.captureFight = false;
    this.rateSuccess = 100;
    this.setTitle("Damage Reward");
  }

  public RewardDamage(String title) {
    super();
    this.captureFight = false;
    this.rateSuccess = 100;
    this.setTitle(title);
  }
}
