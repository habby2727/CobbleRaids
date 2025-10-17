package com.kingpixel.cobbleraids.mixins;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.RaidBall;
import com.kingpixel.cobbleutils.CobbleUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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
    if (!CobbleRaids.config.isRaidBallEnabled()) return;
    var captureSession = CobbleRaids.captureSessionManager.getSessionByPlayer(playerEntity.getUuid());
    ItemStack stack = playerEntity.getStackInHand(hand);
    if (captureSession == null) return;
    try {
      if (!stack.isEmpty() && RaidBall.isRaidBall(stack)) {
        return;
      }
    } catch (Exception e) {
      CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "Error in UseItemCallback event: " + e.getMessage());
      e.printStackTrace();
    }
    cir.cancel();
  }
}
