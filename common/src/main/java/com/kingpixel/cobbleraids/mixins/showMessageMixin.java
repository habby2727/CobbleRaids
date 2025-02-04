package com.kingpixel.cobbleraids.mixins;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.managers.BattleManager;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Date;
import java.util.function.BooleanSupplier;

/**
 * Author: Carlos Varas Alonso - 19/01/2025 1:35
 */
@Mixin(MinecraftServer.class)
public class showMessageMixin {
  @Unique private int cobbleRaids$tickCounter = 0;

  @Inject(method = "tick", at = @At("HEAD"))
  private void tick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
    cobbleRaids$tickCounter++;
    if (cobbleRaids$tickCounter % 20 != 0) return;

    if (CobbleRaids.battleManager != null) {
      // Raid termina en
      BattleManager battleManager = CobbleRaids.battleManager;
      var text = AdventureTranslator.toNative(
        CobbleRaids.language.getMessageRaidFinishing()
          .replace("%cooldown%", PlayerUtils.getCooldown(battleManager.getFinishTime()))
          .replace("%prefix%", CobbleRaids.config.getPrefix())
          .replace("%raid%", battleManager.getRaid().getName())
      );
      cobbleRaids$showMessage(text);
      if (battleManager.getFinishTime() != null && battleManager.getFinishTime().before(new Date())) {
        CobbleRaids.battleManager.finishRaid(false);
        var finishText = AdventureTranslator.toNative(
          CobbleRaids.language.getMessageFinishRaid()
            .replace("%prefix%", CobbleRaids.config.getPrefix()
              .replace("%raid%", battleManager.getRaid().getName()))
        );
        cobbleRaids$showMessage(finishText);
      }
    } else if (CobbleRaids.startDate != null) {
      // Raid empieza en
      long timeLeft = CobbleRaids.startDate.getTime() - System.currentTimeMillis();
      long minutesLeft = timeLeft / 60000;
      if (minutesLeft <= CobbleRaids.config.getStartShowBar()) {
        var text = AdventureTranslator.toNative(
          CobbleRaids.language.getMessageStartingRaid()
            .replace("%cooldown%", PlayerUtils.getCooldown(CobbleRaids.startDate))
            .replace("%prefix%", CobbleRaids.config.getPrefix())
        );
        cobbleRaids$showMessage(text);
      }
      if (timeLeft <= 0) {
        BattleManager.startRaid(null);
        var startText = AdventureTranslator.toNative(
          CobbleRaids.language.getMessageStartRaid()
            .replace("%prefix%", CobbleRaids.config.getPrefix())
        );
        cobbleRaids$showMessage(startText);
      }
    }

    cobbleRaids$tickCounter = 0;
  }

  @Unique
  private void cobbleRaids$showMessage(Text text) {
    for (ServerPlayerEntity player : CobbleRaids.server.getPlayerManager().getPlayerList()) {
      player.sendMessage(text, true);
    }
  }
}