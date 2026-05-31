package com.kingpixel.cobbleraids.mixins;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.RaidBall;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Carlos Varas Alonso - 17/10/2025 6:26
 */
@Mixin(ItemStack.class)
public abstract class RaidBallMixin {
  @Inject(method = "use", at = @At("HEAD"), cancellable = true)
  private void raidBallMixin$use(World world, PlayerEntity playerEntity, Hand hand,
                                 CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
    try {
      if (!CobbleRaids.config.isRaidBallEnabled()) return;
      if (!(playerEntity instanceof ServerPlayerEntity player)) return;
      ItemStack stack = player.getStackInHand(hand);
      if (stack == null) return;
      if (stack.isEmpty()) return;
      var captureSession = CobbleRaids.captureSessionManager.getSessionByPlayer(player.getUuid());
      boolean isRaidBall = RaidBall.isRaidBall(stack);
      boolean cancel = false;
      if (captureSession != null && !isRaidBall) {
        var message = CobbleRaids.language.getMessageRaidBall();
        message.sendMessage(player.getUuid(), CobbleRaids.language.getPrefix(), false);
        cancel = true;
      } else if (captureSession == null && isRaidBall) {
        var message = CobbleRaids.language.getMessageOutCaptureSession();
        message.sendMessage(player.getUuid(), CobbleRaids.language.getPrefix(), false);
        cancel = true;
      } else if (captureSession != null && !captureSession.isStarted()) {
        cancel = true;
      }
      if (cancel) {
        CobbleRaids.captureSessionManager.clearPendingRaidBallCatchRate(player.getUuid());
        cir.setReturnValue(TypedActionResult.fail(stack));
        cir.cancel();
        return;
      }
      if (captureSession != null && captureSession.isStarted() && isRaidBall) {
        var catchChance = RaidBall.getCatchChance(stack);
        if (catchChance != null) {
          CobbleRaids.captureSessionManager.setPendingRaidBallCatchRate(player.getUuid(), catchChance);
        } else {
          CobbleRaids.captureSessionManager.clearPendingRaidBallCatchRate(player.getUuid());
        }
      }
    } catch (Exception e) {
      CobbleRaids.LOGGER.error(CobbleRaids.MOD_ID, "Error in UseItemCallback event: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
