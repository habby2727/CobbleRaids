package com.kingpixel.cobbleraids.mixins;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import net.minecraft.entity.Entity;
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

@Mixin(Entity.class)
public class AttackRaidMixin {
  @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
  public void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
    Entity entity = (Entity) (Object) this;
    if (entity == null) return;
    if (entity instanceof PokemonEntity pokemonEntity) {
      Pokemon pokemon = pokemonEntity.getPokemon();
      if (pokemon != null) {
        NbtCompound nbt = pokemon.getPersistentData();
        if (nbt.getBoolean(CobbleRaids.TAG_RAID) || nbt.getBoolean(CobbleRaids.TAG_FAKERAID)) {
          Entity attacker = source.getAttacker();
          if (attacker instanceof ServerPlayerEntity player) {
            CobbleRaids.language.getMenuRewards().open(player);
          }
          cir.cancel();
        }
      }
    }

  }
}

