package com.kingpixel.cobbleraids.ui;

import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PanelsConfig;
import com.kingpixel.cobbleutils.features.shops.Shop;
import lombok.Getter;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 19/01/2025 1:27
 */
@Getter
public class MenuDamageRewards {
  private int rows;
  private String title;
  private Shop.Rectangle rectangle;
  private ItemModel display;
  private ItemModel previous;
  private ItemModel close;
  private ItemModel next;
  private List<PanelsConfig> panels;

  public MenuDamageRewards() {
    this.rows = 6;
    this.title = "&6Damage Rewards";
    this.rectangle = new Shop.Rectangle(6);
    this.display = new ItemModel(22, "minecraft:iron_sword", "&cDamage Rewards: %pos%", List.of(), 0);
    this.previous = new ItemModel(45, "minecraft:arrow", "&cPrevious", List.of(), 0);
    this.close = new ItemModel(49, "minecraft:barrier", "&cClose", List.of(), 0);
    this.next = new ItemModel(53, "minecraft:arrow", "&aNext", List.of(), 0);
    panels = List.of(new PanelsConfig(new ItemModel("minecraft:gray_stained_glass_pane"), 6));
  }
}
