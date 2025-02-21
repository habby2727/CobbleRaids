package com.kingpixel.cobbleraids.rewards;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.model.CaptureSession;
import com.kingpixel.cobbleraids.model.PokemonRaid;
import com.kingpixel.cobbleraids.ui.MenuDamageRewards;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PanelsConfig;
import com.kingpixel.cobbleutils.Model.Rectangle;
import com.kingpixel.cobbleutils.util.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * Improved by GitHub Copilot
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Data
public class DamageRewards extends RaidRewards {
  private Map<String, RewardDamage> rewards;

  public DamageRewards() {
    super();
    this.rewards = new HashMap<>();
    // Example rewards setup
    rewards.put("1", new RewardDamage("First Position"));
    rewards.put("2", new RewardDamage("Second Position"));
    rewards.put("3", new RewardDamage("Third Position"));
    rewards.put("4-7", new RewardDamage("4th to 7th Position"));
    rewards.put("8-100", new RewardDamage("8th to 100th Position"));
  }

  public void giveRewards(Map<UUID, Integer> players, PokemonRaid pokemonRaid) {
    try {
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

        int pos = sortedPlayers.indexOf(sortedPlayer) + 1; // Position is 1-base
        for (Map.Entry<String, RewardDamage> reward : rewards.entrySet()) {
          String[] split = reward.getKey().split("-");
          int min = Integer.parseInt(split[0]);
          int max = split.length > 1 ? Integer.parseInt(split[1]) : min;
          if (pos >= min && pos <= max) {
            RewardDamage rewardDamage = reward.getValue();
            rewardDamage.giveRewards(player);
            if (rewardDamage.isCaptureFight()) {
              var battle = Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player);
              if (battle != null) {
                battle.setEnded(true);
                battle.end();
              }
              if (rewardDamage.getRateSuccess() <= 0 || Utils.RANDOM.nextInt(rewardDamage.getRateSuccess()) == 0) {
                PlayerUtils.sendMessage(
                  player,
                  CobbleRaids.language.getMessageCaptureSessionLuck(),
                  CobbleRaids.config.getPrefix(),
                  TypeMessage.CHAT
                );
                CaptureSession.activeCaptures.add(new CaptureSession(pokemonRaid, player));
              } else {
                PlayerUtils.sendMessage(
                  player,
                  CobbleRaids.language.getMessageCaptureSessionNotLuck(),
                  CobbleRaids.config.getPrefix(),
                  TypeMessage.CHAT
                );
              }
            }
            sendInfo(playerUUID, pos);
            break;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  private void sendInfo(UUID playerUUID, int pos) {
    ServerPlayerEntity player = CobbleRaids.server.getPlayerManager().getPlayer(playerUUID);
    if (player == null) return;
    PlayerUtils.sendMessage(
      player,
      CobbleRaids.language.getMessageRewardDamage()
        .replace("%pos%", String.valueOf(pos)),
      CobbleRaids.config.getPrefix(),
      TypeMessage.CHAT
    );
  }

  @Override public void open(ServerPlayerEntity player, String raid) {
    MenuDamageRewards menu = CobbleRaids.language.getMenuDamageRewards();
    int rows = menu.getRows();
    String title = menu.getTitle();
    ItemModel previous = menu.getPrevious();
    ItemModel close = menu.getClose();
    ItemModel next = menu.getNext();
    List<PanelsConfig> panels = menu.getPanels();
    Rectangle rectangle = menu.getRectangle();
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
        CobbleRaids.language.getMenuRewards().open(action.getPlayer(), raid);
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
          open(player, raid);
        });
      });
      buttons.add(button);
    });

    GooeyPage page = PaginationHelper.createPagesFromPlaceholders(template, buttons, null);
    page.setTitle(AdventureTranslator.toNative(title));

    UIManager.openUIForcefully(player, page);
  }

  public void check() {
    Map<String, RewardDamage> updatedRewards = new HashMap<>();
    for (Map.Entry<String, RewardDamage> entry : rewards.entrySet()) {
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