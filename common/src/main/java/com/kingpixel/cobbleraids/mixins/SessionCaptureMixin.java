package com.kingpixel.cobbleraids.mixins;

import com.kingpixel.cobbleraids.model.CaptureSession;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * @author Carlos Varas Alonso - 06/02/2025 4:00
 */

@Mixin(MinecraftServer.class)
public class SessionCaptureMixin {
  @Unique private int cobbleRaids$tickCounter = 0;


  @Inject(method = "tick", at = @At("HEAD"))
  private void tick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
    try {
      cobbleRaids$tickCounter++;
      if (cobbleRaids$tickCounter % 20 != 0) return;

      if (CaptureSession.activeCaptures.isEmpty()) return;

      List<CaptureSession> toRemove = new ArrayList<>();
      for (CaptureSession session : CaptureSession.activeCaptures) {
        if (session.getInitInSeconds() <= 0) {
          if (!session.isStarted()) {
            session.initCaptureFight();
          }
          if (session.getFinishInSeconds() <= 0) {
            session.finishCaptureFight();
            toRemove.add(session);
          } else {
            session.sendFinishMessage();
          }
        } else {
          session.sendInitMessage();
        }
      }
      CaptureSession.activeCaptures.removeAll(toRemove);
      cobbleRaids$tickCounter = 0;
    } catch (Exception ignored) {
    }

  }

}
