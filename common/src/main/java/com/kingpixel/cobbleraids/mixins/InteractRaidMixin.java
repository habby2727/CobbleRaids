package com.kingpixel.cobbleraids.mixins;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.Raid;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 13/09/2025 5:30
 */
@Mixin(PokemonEntity.class)
public abstract class InteractRaidMixin {
  @Unique private boolean cobbleRaids$raid = false;
  @Shadow private Pokemon pokemon;

  @Inject(method = "interactMob", at = @At("HEAD"))
  private void InteractRaid$onInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
    PokemonEntity self = (PokemonEntity) (Object) this;
    if (self == null || pokemon == null || player == null) return;
    var persistentData = pokemon.getPersistentData();
    if (persistentData == null) return;
    if (!persistentData.contains(Raid.RAID_NBT_KEY)) return;
    UUID raidUUID = persistentData.getUuid(Raid.RAID_NBT_KEY);
    if (raidUUID == null) return;
    var raid = CobbleRaids.raidManager.getRaid(raidUUID);
    if (raid == null) {
      self.discard();
      return;
    }
    raid.openStartBattleMenu((ServerPlayerEntity) player);
  }

  @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
  private void InteractRaid$onDamage(net.minecraft.entity.damage.DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
    if (source.isSourceCreativePlayer()) return;
    cobbleRaids$shouldDamage(cir);
  }

  @Unique private void cobbleRaids$shouldDamage(CallbackInfoReturnable<Boolean> cir) {
    PokemonEntity self = (PokemonEntity) (Object) this;
    if (self == null) return;
    if (cobbleRaids$raid) cir.setReturnValue(false);
    if (pokemon == null) return;
    var persistentData = pokemon.getPersistentData();
    if (persistentData == null) return;
    if (persistentData.contains(Raid.RAID_NBT_KEY)) {
      cobbleRaids$raid = true;
      cir.cancel();
    }
  }
}
