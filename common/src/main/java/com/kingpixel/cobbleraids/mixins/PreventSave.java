package com.kingpixel.cobbleraids.mixins;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.model.Raid;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Carlos Varas Alonso - 10/02/2025 3:48
 */
@Mixin(PokemonEntity.class)
public class PreventSave {

  @Inject(method = "shouldSave", at = @At("HEAD"), cancellable = true)
  private void shouldSave(CallbackInfoReturnable<Boolean> cir) {
    LivingEntity livingEntity = (LivingEntity) (Object) this;
    if (livingEntity == null) return;
    if (livingEntity instanceof PokemonEntity pokemonEntity) {
      if (pokemonEntity == null) {
        cir.setReturnValue(false);
        return;
      }
      Pokemon pokemon = pokemonEntity.getPokemon();
      if (pokemon == null) {
        cir.setReturnValue(false);
        return;
      }
      if (Raid.isRaid(pokemon)) {
        cir.setReturnValue(false);
      }

    }
  }
}
