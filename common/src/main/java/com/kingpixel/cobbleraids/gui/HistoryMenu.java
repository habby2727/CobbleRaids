package com.kingpixel.cobbleraids.gui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleraids.database.DataBaseFactory;
import com.kingpixel.cobbleraids.models.Raid;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.UIUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 16/09/2025 22:01
 */
public class HistoryMenu {
  public static void open(ServerPlayerEntity player, int numPage) {
    ChestTemplate template = ChestTemplate.builder(6)
      .build();


    var raids = DataBaseFactory.INSTANCE.getHistoryRaids(1, 36);

    List<GooeyButton> buttons = new ArrayList<>();
    for (var raid : raids) {
      buttons.add(getButton(raid));
    }

    template.set(53, UIUtils.getNextButton(action -> {
      open(player, numPage + 1);
    }));

    template.set(45, UIUtils.getPreviousButton(action -> {
        if (numPage > 1) {
          open(player, numPage - 1);
        }
      })
    );

    GooeyPage page = GooeyPage.builder()
      .title(AdventureTranslator.toNative("Raid History"))
      .template(template)
      .build();

    UIManager.openUIForcefully(player, page);
  }

  @SuppressWarnings("null")
  private static GooeyButton getButton(Raid raid) {
    long start = raid.getStartTime();
    Instant startInstant = Instant.ofEpochMilli(start);
    List<String> lore = new ArrayList<>(
      List.of(
        "Start: " + startInstant.toString(),
        "End: " + (raid.getEndTime() == 0 ? "Ongoing" : Instant.ofEpochMilli(raid.getEndTime()).toString()),
        "Players: " + raid.getDamageMap().size()
      )
    );
    return GooeyButton.builder()
      .display(Items.BOOK.getDefaultStack())
      .with(DataComponentTypes.CUSTOM_NAME,
        AdventureTranslator.toNative(
          " Raid"
        )
      )
      .with(DataComponentTypes.LORE,
        new LoreComponent(
          AdventureTranslator.toNativeL(lore)
        ))
      .build();

  }
}
