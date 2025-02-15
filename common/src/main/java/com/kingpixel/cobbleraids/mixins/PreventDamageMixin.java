package com.kingpixel.cobbleraids.mixins;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.managers.BattleManager;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.PermissionApi;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 18/01/2025 18:39
 */

@Mixin(LivingEntity.class)
public class PreventDamageMixin {

  @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
  private void damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
    try {
      LivingEntity livingEntity = (LivingEntity) (Object) this;
      if (livingEntity == null) return;
      if (livingEntity instanceof PokemonEntity pokemonEntity) {
        NbtCompound nbt = pokemonEntity.getPokemon().getPersistentData();
        if (nbt.getBoolean(CobbleRaids.TAG_RAID) || nbt.getBoolean(CobbleRaids.TAG_FAKERAID)) {
          var activeRaid = BattleManager.getActiveRaid(pokemonEntity.getUuid());
          if (activeRaid == null) {
            pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
            cir.cancel();
            return;
          }
          Entity attacker = source.getAttacker();
          if (attacker == null) return;
          if (attacker instanceof ServerPlayerEntity player) {
            if (!PermissionApi.hasPermission(player, List.of(CobbleRaids.MOD_ID + ".admin", CobbleRaids.MOD_ID +
              ".rewards"), 2)) {
              cir.cancel();
              return;
            }
            String id = nbt.getString(CobbleRaids.TAG_RAID_ID);
            if (id != null && !id.isEmpty()) {
              CobbleRaids.language.getMenuRewards().open(player, id);
            } else {
              CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID + " - Error: Raid ID is null or empty");
            }
          }
          cir.cancel();
        }
      }
    } catch (Exception ignored) {
    }
  }
}

