package com.kingpixel.cobbleraids.mixins;

import com.cobblemon.mod.common.api.drop.DropTable;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.kingpixel.cobbleraids.CobbleRaids;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents item drops and handles raid Pokemon interactions.
 *
 * @author Carlos
 */
@Mixin(PokemonEntity.class)
public abstract class PokemonPreventDropMixin {
  @Inject(method = "getDrops", at = @At("HEAD"), cancellable = true, remap = false)
  private void cobbleRaids$onGetDrops(CallbackInfoReturnable<DropTable> cir) {
    try {
      PokemonEntity entity = (PokemonEntity) (Object) this;
      var fightData = CobbleRaids.raidManager.getFightingData(entity.getBattleId());
      if (fightData != null) {
        cir.setReturnValue(new DropTable());
      }
    } catch (Exception e) {
      CobbleRaids.LOGGER.error("Error in PreventDropMixin getDrops: " + e.getMessage());
      e.printStackTrace();
    }
  }

  @Inject(method = "updatePostDeath", at = @At("HEAD"))
  private void cobbleRaids$onDeath(CallbackInfo ci) {
    try {
      PokemonEntity pokemonEntity = (PokemonEntity) (Object) this;
      var fightData = CobbleRaids.raidManager.getFightingData(pokemonEntity.getBattleId());
      if (fightData != null) {
        pokemonEntity.teleport(pokemonEntity.getX(), -1000, pokemonEntity.getZ(), false);
      }
    } catch (Exception e) {
      CobbleRaids.LOGGER.error("Error in PreventDropMixin: " + e.getMessage());
      e.printStackTrace();
    }
  }

  @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
  private void cobbleRaids$onDrop(ServerWorld world, DamageSource source, CallbackInfo ci) {
    try {
      LivingEntity entity = (LivingEntity) (Object) this;
      if (entity instanceof PokemonEntity pokemonEntity) {
        var fightData = CobbleRaids.raidManager.getFightingData(pokemonEntity.getBattleId());
        if (fightData != null) {
          pokemonEntity.teleport(pokemonEntity.getX(), -1000, pokemonEntity.getZ(), false);
          ci.cancel();
        }
      }
    } catch (Exception e) {
      CobbleRaids.LOGGER.error("Error in PreventDropMixin: " + e.getMessage());
      e.printStackTrace();
    }
  }
  
}
