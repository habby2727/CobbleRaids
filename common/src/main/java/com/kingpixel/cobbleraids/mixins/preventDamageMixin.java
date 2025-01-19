package com.kingpixel.cobbleraids.mixins;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.kingpixel.cobbleraids.CobbleRaids;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Carlos Varas Alonso - 18/01/2025 18:39
 */

@Mixin(LivingEntity.class)
public class preventDamageMixin {


  @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
  private void damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
    try {
      LivingEntity livingEntity = (LivingEntity) (Object) this;

      if (livingEntity instanceof PokemonEntity pokemonEntity) {
        NbtCompound nbt = pokemonEntity.getPokemon().getPersistentData();
        if (nbt.getBoolean(CobbleRaids.TAG_RAID) || nbt.getBoolean(CobbleRaids.TAG_FAKERAID)) {
          cir.setReturnValue(false);
          Entity attacker = source.getAttacker();
          if (attacker instanceof ServerPlayerEntity player) {
            CobbleRaids.language.getMenuRewards().open(player);
          }
        }
      }
    } catch (Exception ignored) {
    }
  }
}

