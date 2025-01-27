package com.kingpixel.cobbleraids.rewards;

import com.kingpixel.cobbleutils.Model.AdvancedItemChance;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Carlos Varas Alonso - 25/01/2025 5:21
 */
@Getter
@Setter
@ToString
public class RewardDamage extends AdvancedItemChance {
  private boolean captureFight;

  public RewardDamage() {
    super();
    this.captureFight = false;
    this.setTitle("Damage Reward");
  }

  public RewardDamage(String title) {
    super();
    this.captureFight = false;
    this.setTitle(title);
  }
}
