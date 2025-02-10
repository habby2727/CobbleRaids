package com.kingpixel.cobbleraids.mixins;

import com.cobblemon.mod.common.api.pokemon.experience.StandardExperienceCalculator;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.model.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


@Mixin(StandardExperienceCalculator.class)
public class ExpGainMixin {


  @ModifyVariable(method = "calculate", at = @At("STORE"), name = "term4", remap = false)
  private double injectedExpGainLock(double term4, BattlePokemon battlePokemon, BattlePokemon opponentPokemon) {
    if (!CobbleRaids.config.isBlockXp()) return term4;
    var opponent = opponentPokemon.getOriginalPokemon();
    if (Raid.isRaid(opponent)) return 0;
    return term4;
  }
}