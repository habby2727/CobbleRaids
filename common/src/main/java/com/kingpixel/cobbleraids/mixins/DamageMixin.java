package com.kingpixel.cobbleraids.mixins;

import com.cobblemon.mod.common.api.battles.interpreter.BattleMessage;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.interpreter.instructions.DamageInstruction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Carlos Varas Alonso - 16/09/2025 14:55
 */
@Mixin(DamageInstruction.class)
public class DamageMixin {
  @Final
  @Shadow(remap = false)
  private BattleMessage publicMessage;

  @Final
  @Shadow(remap = false)
  private BattleMessage privateMessage;

  @Final
  @Shadow(remap = false)
  private BattleActor actor;

  @Inject(method = "postActionEffect", at = @At("HEAD"), cancellable = true, remap = false)
  private void postActionEffectInject(PokemonBattle battle, CallbackInfo ci) {
    /*BattlePokemon battlePokemon = publicMessage.battlePokemon(0, actor.battle);
    if (battlePokemon == null) return;
    if (battlePokemon.getEntity() == null) return;
    UUID battleUUID = battle.getBattleId();
    if (battleUUID == null) return;

    String newHealth = Objects.requireNonNull(this.privateMessage.argumentAt(1)).split(" ")[0];
    if (newHealth.equals("0")) {
      battlePokemon.getEffectedPokemon().setCurrentHealth(0);
      //raidInstance.queueStopRaid();
    } else {
      float remainingHealth = Float.parseFloat(newHealth.split("/")[0]);
      if (CobbleRaids.config.isDebug()) {
        CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID,
          "DamageMixin - postActionEffectInject - battleUUID: " + battleUUID + " - remainingHealth: " + remainingHealth +
            " - newHealth: " + newHealth);
      }
      Raid raid = CobbleRaids.raidManager.getFightingData(battleUUID).getRaid();
      raid.setHealth((int) remainingHealth);

      //battle.getPlayers().forEach(player -> raidInstance.syncHealth(player, remainingHealth));
      //battlePokemon.getEffectedPokemon().setCurrentHealth((int) raidInstance.getRemainingHealth());
    }
    ci.cancel();*/
  }
}
