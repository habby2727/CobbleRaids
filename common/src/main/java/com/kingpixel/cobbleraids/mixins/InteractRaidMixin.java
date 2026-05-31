package com.kingpixel.cobbleraids.mixins;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.Raid;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Prevents item drops and handles raid Pokemon interactions.
 *
 * @author Carlos
 */
@Mixin(PokemonEntity.class)
public abstract class InteractRaidMixin {

  @Unique private boolean cobbleRaids$raid = false;

  // Handle player interaction.
  @Inject(method = "interactMob", at = @At("HEAD"))
  private void cobbleRaids$onInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
    try {
      PokemonEntity self = (PokemonEntity) (Object) this;
      Pokemon pokemon = self.getPokemon();
      if (player == null) return;

      var persistentData = pokemon.getPersistentData();
      if (!persistentData.contains(Raid.RAID_NBT_KEY)) return;

      UUID raidUUID = persistentData.getUuid(Raid.RAID_NBT_KEY);
      if (raidUUID == null) return;

      var raid = CobbleRaids.raidManager.getRaid(raidUUID);

      if (raid == null) {
        self.discard(); // Se elimina si el raid ya no existe
        return;
      }

      raid.openStartBattleMenu((ServerPlayerEntity) player);

    } catch (Exception e) {
      CobbleRaids.LOGGER.error(CobbleRaids.MOD_ID, "Error in InteractRaidMixin onInteract: " + e.getMessage());
    }
  }

  // Prevent raid Pokemon from being damaged.
  @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
  private void cobbleRaids$onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
    try {
      PokemonEntity self = (PokemonEntity) (Object) this;
      Pokemon pokemon = self.getPokemon();
      var persistentData = pokemon.getPersistentData();
      if (cobbleRaids$isRaidPokemon() || persistentData.contains(Raid.RAID_NBT_KEY)) {
        cir.cancel();
      }
    } catch (Exception e) {
      CobbleRaids.LOGGER.error(CobbleRaids.MOD_ID, "Error in InteractRaidMixin onDamage: " + e.getMessage());
      e.printStackTrace();
    }
  }


  @Unique private boolean cobbleRaids$isRaidPokemon() {
    PokemonEntity self = (PokemonEntity) (Object) this;
    Pokemon pokemon = self.getPokemon();
    if (cobbleRaids$raid) return true;

    var persistentData = pokemon.getPersistentData();
    if (persistentData.contains(Raid.RAID_NBT_KEY)) {
      cobbleRaids$raid = true;
      return true;
    }

    return cobbleRaids$isBattleRaid(pokemon);
  }

  @Unique private boolean cobbleRaids$isBattleRaid(Pokemon pokemon) {
    var entity = pokemon.getEntity();
    if (entity == null) {
      return false;
    }

    var battleId = entity.getBattleId();
    if (battleId == null) {
      return false;
    }

    return CobbleRaids.raidManager.getFightingData(battleId) != null || CobbleRaids.captureSessionManager.getActiveSessions().get(battleId) != null;
  }
}
