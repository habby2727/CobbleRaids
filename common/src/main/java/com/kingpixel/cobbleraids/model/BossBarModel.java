package com.kingpixel.cobbleraids.model;

import lombok.Getter;
import net.minecraft.entity.boss.BossBar;

/**
 * @author Carlos Varas Alonso - 18/01/2025 23:25
 */
@Getter
public class BossBarModel {
  private String title;
  private BossBar.Color color;
  private BossBar.Style style;

  public BossBarModel() {
    this.title = "Raid Boss -> %boss% <- %hp% / %max_hp%";
    this.color = BossBar.Color.WHITE;
    this.style = BossBar.Style.PROGRESS;
  }
}
