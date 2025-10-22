package com.kingpixel.cobbleraids.mixins;

import com.cobblemon.mod.common.api.pokemon.experience.StandardExperienceCalculator;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * @author Carlos Varas Alonso - 05/10/2025 3:05
 */
@Mixin(StandardExperienceCalculator.class)
public abstract class PreventXPMixin {
  @ModifyVariable(method = "calculate", at = @At("STORE"), name = "term4", remap = false)
  private double injectedExpGainLock(double term4, BattlePokemon battlePokemon, BattlePokemon opponentPokemon) {
    if (!CobbleRaids.config.isXpBlock()) return term4;
    var opponent = opponentPokemon.getOriginalPokemon();
    if (cobbleRaids$isRaidPokemon(opponent) && CobbleRaids.config.isXpBlock()) return 0;
    return term4;
  }

  @Unique private boolean cobbleRaids$isRaidPokemon(Pokemon pokemon) {
    var persistentData = pokemon.getPersistentData();
    if (persistentData.contains(Raid.RAID_NBT_KEY)) return true;
    return cobbleRaids$isBattleRaid(pokemon);
  }

  @Unique private boolean cobbleRaids$isBattleRaid(Pokemon pokemon) {
    var entity = pokemon.getEntity();
    if (entity == null) return false;
    var battleId = entity.getBattleId();
    if (battleId == null) return false;
    return CobbleRaids.raidManager.getFightingData(battleId) != null;
  }
}
