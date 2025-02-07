package com.kingpixel.cobbleraids.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.model.Raid;
import com.kingpixel.cobbleraids.rewards.DamageRewards;
import com.kingpixel.cobbleraids.rewards.GlobalRewards;
import com.kingpixel.cobbleraids.rewards.LastHitRewards;
import com.kingpixel.cobbleraids.rewards.RaidRewards;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PanelsConfig;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.UIUtils;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 18/01/2025 1:34
 */
public class MenuRewards {
  private int rows;
  private String title;
  private ItemModel damageRewards;
  private ItemModel globalRewards;
  private ItemModel killRewards;
  private ItemModel close;
  private List<PanelsConfig> panels;

  public MenuRewards() {
    this.rows = 3;
    this.title = "&6Rewards %raid%";
    this.damageRewards = new ItemModel(11, "minecraft:iron_sword", "&cDamage Rewards", List.of(), 0);
    this.killRewards = new ItemModel(13, "minecraft:bone", "&cKill Rewards", List.of(), 0);
    this.globalRewards = new ItemModel(15, "minecraft:end_crystal", "&aGlobal Rewards", List.of(), 0);
    this.close = new ItemModel(22, "minecraft:barrier", "&cClose", List.of(), 0);
    panels = new ArrayList<>();
    panels.add(new PanelsConfig(new ItemModel("minecraft:gray_stained_glass_pane"), 3));
  }

  public void open(ServerPlayerEntity player, String s) {
    ChestTemplate template = ChestTemplate
      .builder(rows)
      .build();

    PanelsConfig.applyConfig(template, panels);
    Raid raid = Raid.getRaid(s);
    if (raid == null) {
      CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID + " Raid not found: " + s);
      return;
    }
    for (RaidRewards reward : raid.getRewards()) {
      if (!reward.isActive()) continue;
      switch (reward) {
        case DamageRewards DamageRewards -> {
          if (UIUtils.isInside(damageRewards, rows)) {
            template.set(damageRewards.getSlot(), damageRewards.getButton(action -> {
              DamageRewards.open(player, s);
            }));

          }
        }
        case LastHitRewards KillRewards -> {
          if (UIUtils.isInside(killRewards, rows)) {
            template.set(killRewards.getSlot(), killRewards.getButton(action -> {
              KillRewards.open(player, s);
            }));
          }
        }
        case GlobalRewards GlobalRewards -> {
          if (UIUtils.isInside(globalRewards, rows)) {
            template.set(globalRewards.getSlot(), globalRewards.getButton(action -> {
              GlobalRewards.open(player, s);
            }));
          }
        }
        default -> {
        }
      }
    }


    if (UIUtils.isInside(close, rows)) {
      template.set(close.getSlot(), close.getButton(action -> UIManager.closeUI(player)));
    }

    GooeyPage page = GooeyPage.builder()
      .template(template)
      .title(AdventureTranslator.toNative(title
        .replace("%raid%", raid.getId())))
      .build();

    UIManager.openUIForcefully(player, page);
  }
}
