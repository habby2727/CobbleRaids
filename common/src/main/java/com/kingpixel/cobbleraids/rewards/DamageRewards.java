package com.kingpixel.cobbleraids.rewards;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.ui.MenuDamageRewards;
import com.kingpixel.cobbleutils.Model.AdvancedItemChance;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PanelsConfig;
import com.kingpixel.cobbleutils.features.shops.Shop;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.kingpixel.cobbleutils.util.UIUtils;
import lombok.Getter;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * Improved by GitHub Copilot
 */
@Getter
public class DamageRewards extends RaidRewards {
  private Map<String, AdvancedItemChance> rewards;

  public DamageRewards() {
    super();
    this.rewards = new HashMap<>();
    // Example rewards setup
    rewards.put("1", new AdvancedItemChance());
    rewards.put("2", new AdvancedItemChance());
    rewards.put("3", new AdvancedItemChance());
    rewards.put("4-7", new AdvancedItemChance());
    rewards.put("8-100", new AdvancedItemChance());
    rewards.forEach((key, value) -> {
      value.setTitle("Damage Reward Position -> " + key);
    });
  }

  @Override
  public void giveRewards(Map<UUID, Integer> players) {
    if (!active) return;
    if (players.isEmpty()) return;
    // Sort players by damage in descending order
    List<Map.Entry<UUID, Integer>> sortedPlayers = players.entrySet().stream()
      .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
      .toList();

    for (Map.Entry<UUID, Integer> sortedPlayer : sortedPlayers) {
      UUID playerUUID = sortedPlayer.getKey();
      ServerPlayerEntity player = CobbleRaids.server.getPlayerManager().getPlayer(playerUUID);
      if (player == null) continue;
      int pos = sortedPlayers.indexOf(sortedPlayer) + 1; // Position is 1-based
      for (Map.Entry<String, AdvancedItemChance> reward : rewards.entrySet()) {
        String[] split = reward.getKey().split("-");
        int min = Integer.parseInt(split[0]);
        int max = split.length > 1 ? Integer.parseInt(split[1]) : min;
        if (pos >= min && pos <= max) {
          AdvancedItemChance itemChance = reward.getValue();
          itemChance.giveRewards(player);
          sendInfo(playerUUID, pos);
          break;
        }
      }
    }
  }

  private void sendInfo(UUID playerUUID, int pos) {
    ServerPlayerEntity player = CobbleRaids.server.getPlayerManager().getPlayer(playerUUID);
    if (player == null) return;
    PlayerUtils.sendMessage(
      player,
      CobbleRaids.language.getMessageRewardDamage()
        .replace("%pos%", String.valueOf(pos + 1)),
      CobbleRaids.config.getPrefix(),
      TypeMessage.CHAT
    );
  }

  @Override public void open(ServerPlayerEntity player) {
    MenuDamageRewards menu = CobbleRaids.language.getMenuDamageRewards();
    int rows = menu.getRows();
    String title = menu.getTitle();
    ItemModel previous = menu.getPrevious();
    ItemModel close = menu.getClose();
    ItemModel next = menu.getNext();
    List<PanelsConfig> panels = menu.getPanels();
    Shop.Rectangle rectangle = menu.getRectangle();
    ChestTemplate template = ChestTemplate
      .builder(rows)
      .build();


    PanelsConfig.applyConfig(template, panels);
    rectangle.apply(template);

    if (UIUtils.isInside(previous, rows)) {
      template.set(previous.getSlot(), LinkedPageButton.builder()
        .display(previous.getItemStack())
        .linkType(LinkType.Previous)
        .build());
    }

    if (UIUtils.isInside(close, rows)) {
      template.set(close.getSlot(), close.getButton(action -> {
        CobbleRaids.language.getMenuRewards().open(action.getPlayer());
      }));
    }

    if (UIUtils.isInside(next, rows)) {
      template.set(next.getSlot(), LinkedPageButton.builder()
        .display(next.getItemStack())
        .linkType(LinkType.Next)
        .build());
    }

    List<Button> buttons = new ArrayList<>();
    rewards.forEach((key, value) -> {
      String name = menu.getDisplay().getDisplayname()
        .replace("%pos%", key);
      GooeyButton button = menu.getDisplay().getButton(1, name, action -> {
        value.openMenu(player, template1 -> {
        }, close1 -> {
          open(player);
        });
      });
      buttons.add(button);
    });

    GooeyPage page = PaginationHelper.createPagesFromPlaceholders(template, buttons, null);
    page.setTitle(AdventureTranslator.toNative(title));

    UIManager.openUIForcefully(player, page);
  }

  public void check() {
    Map<String, AdvancedItemChance> updatedRewards = new HashMap<>();
    for (Map.Entry<String, AdvancedItemChance> entry : rewards.entrySet()) {
      String key = entry.getKey();
      if (key.contains("=") || key.contains(">=")) {
        updatedRewards.put(key
          .replace(">=", "-100")
          .replace("=", ""), entry.getValue());
      } else {
        updatedRewards.put(key, entry.getValue());
      }
    }
    rewards = updatedRewards;
  }
}