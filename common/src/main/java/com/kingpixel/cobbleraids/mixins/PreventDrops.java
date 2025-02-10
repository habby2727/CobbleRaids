package com.kingpixel.cobbleraids.mixins;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.CobbleUtils;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Carlos Varas Alonso - 26/01/2025 5:21
 */
@Mixin(PokemonEntity.class)
public class PreventDrops {


  @Inject(method = "updatePostDeath", at = @At("HEAD"))
  private void onPostDeath(CallbackInfo ci) {
    try {
      PokemonEntity pokemonEntity = (PokemonEntity) (Object) this;
      if (cobbleRaids$isRaid(pokemonEntity)) {
        pokemonEntity.teleport(pokemonEntity.getX(), -1000, pokemonEntity.getZ(), false);
      }
    } catch (Exception ignored) {
    }
  }

  @Unique
  private boolean cobbleRaids$isRaid(PokemonEntity pokemonEntity) {
    if (pokemonEntity == null) {
      if (CobbleRaids.config.isDebug()) {
        CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, CobbleRaids.MOD_ID + " - Error: PokemonEntity is null");
      }
      return false;
    }
    Pokemon pokemon = pokemonEntity.getPokemon();
    NbtCompound nbt = pokemon.getPersistentData();
    if (nbt.getBoolean(CobbleRaids.TAG_RAID) || nbt.getBoolean(CobbleRaids.TAG_FAKERAID)) {
      pokemon.removeHeldItem();
      return true;
    }
    return false;
  }
}
