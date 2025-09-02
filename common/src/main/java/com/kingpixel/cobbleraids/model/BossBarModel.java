package com.kingpixel.cobbleraids.model;

import lombok.Getter;
import net.minecraft.entity.boss.BossBar;

/**
 * @author Carlos Varas Alonso - 18/01/2025 23:25
 */
@Getter
public class BossBarModel {
  private final String title;
  private final BossBar.Color color;
  private final BossBar.Style style;

  public BossBarModel() {
    this.title = "§cR§6a§ei§ad §bB§9o§5s§ds -> §c%boss% §r<- §a%hp% §r/ §a%hp_max%";
    this.color = BossBar.Color.PINK;
    this.style = BossBar.Style.NOTCHED_10;
  }
}
