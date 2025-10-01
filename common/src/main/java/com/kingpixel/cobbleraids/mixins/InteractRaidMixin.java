package com.kingpixel.cobbleraids.mixins;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.Raid;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Evita drops de ítems y maneja interacciones con Pokémon en raids.
 *
 * @author Carlos
 */
@Mixin(PokemonEntity.class)
public abstract class InteractRaidMixin {

  @Unique private boolean cobbleRaids$raid = false;

  // Evitar drop al morir
  @Inject(method = "updatePostDeath", at = @At("HEAD"))
  private void cobbleRaids$onUpdatePostDeath(CallbackInfo ci) {
    cobbleRaids$stopRaidDrop();
  }

  // Cancelar drops directamente
  @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
  private void cobbleRaids$onDrop(ServerWorld world, DamageSource source, CallbackInfo ci) {
    if (cobbleRaids$stopRaidDrop()) ci.cancel();
  }

  // Manejar interacción con jugador
  @Inject(method = "interactMob", at = @At("HEAD"))
  private void cobbleRaids$onInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
    PokemonEntity self = (PokemonEntity) (Object) this;
    if (self == null) return;
    Pokemon pokemon = self.getPokemon();
    if (pokemon == null || player == null) return;

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
  }

  // Evitar que se pueda dañar el Pokémon de raid
  @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
  private void cobbleRaids$onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
    if (source.isSourceCreativePlayer()) return;
    if (cobbleRaids$isRaidPokemon()) cir.cancel();
  }

  // --- Métodos auxiliares ---

  @Unique private boolean cobbleRaids$stopRaidDrop() {
    if (cobbleRaids$isRaidPokemon()) {
      PokemonEntity self = (PokemonEntity) (Object) this;
      // Lo mandamos a Y = -1000 para "enterrarlo" y evitar drops
      self.teleport(self.getX(), -1000, self.getZ(), false);
      return true;
    }
    return false;
  }

  @Unique private boolean cobbleRaids$isRaidPokemon() {
    PokemonEntity self = (PokemonEntity) (Object) this;
    Pokemon pokemon = self.getPokemon();
    if (cobbleRaids$raid) return true;
    if (pokemon == null) return false;

    var persistentData = pokemon.getPersistentData();
    if (persistentData.contains(Raid.RAID_NBT_KEY)) {
      cobbleRaids$raid = true;
      return true;
    }

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
