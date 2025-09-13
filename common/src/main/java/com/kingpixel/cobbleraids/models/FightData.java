package com.kingpixel.cobbleraids.models;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 13/09/2025 4:53
 */
@Data
public class FightData {
  private PokemonEntity pokemonEntity;
  private ServerPlayerEntity player;
  private Raid raid;
  private UUID battleUUID;

  public FightData(UUID battleUUID, Raid raid, ServerPlayerEntity player, PokemonEntity pokemonEntity) {
    this.battleUUID = battleUUID;
    this.raid = raid;
    this.player = player;
    this.pokemonEntity = pokemonEntity;
  }

  public void stop() {
    if (CobbleRaids.config.isDebug()) {
      CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "Stopping fight for player " + player.getName().getString() + " and raid " + raid.getRaidUUID());
    }
    CobbleRaids.server.execute(() -> pokemonEntity.discard());
    CobbleRaids.raidManager.removeFightingData(battleUUID);
    var battle = Cobblemon.INSTANCE.getBattleRegistry().getBattle(battleUUID);
    if (battle != null) CobbleRaids.server.execute(battle::stop);
  }
}
