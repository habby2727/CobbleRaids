package com.kingpixel.cobbleraids.models;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.util.ModRandom;
import com.kingpixel.cobblesize.Model.SizeChance;
import lombok.Data;
import net.fabricmc.loader.api.FabricLoader;
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

  public void stop(boolean stopBattle) {
    CobbleRaids.server.execute(() -> {
      try {
        var fight = CobbleRaids.raidManager.removeFightingData(battleUUID);
        if (stopBattle) CobbleRaids.stopBattleSafely(battleUUID, "fight-stop");
        if (pokemonEntity != null) pokemonEntity.discard();
        if (fight != null) {
          if (FabricLoader.getInstance().isModLoaded("cobblesize")) {
            var party = Cobblemon.INSTANCE.getStorage().getParty(player);
            for (Pokemon p : party) {
              SizeChance.solveSize(p);
            }
          }
        }
      } catch (Exception e) {
        String playerName = player == null ? "unknown" : player.getName().getString();
        String raidId = raid == null ? "unknown" : raid.getRaidUUID().toString();
        CobbleRaids.LOGGER.error("Error stopping fight for player " + playerName + " and raid " + raidId + ": " + e.getMessage());
        e.printStackTrace();
      }
    });
  }

  public void teleportPokemon(PokemonEntity targetPokemon) {
    if (pokemonEntity == null || targetPokemon == null) return;

    var world = pokemonEntity.getWorld();
    if (world == null || world.isClient) return;

    // Posición base: el Pokémon principal de la FightData
    var basePos = pokemonEntity.getBlockPos();

    // Calcular desplazamiento aleatorio a unos 32 bloques de distancia
    int distance = 32;
    double angle = ModRandom.current().nextDouble() * (2 * Math.PI);
    double offsetX = Math.cos(angle) * distance;
    double offsetZ = Math.sin(angle) * distance;

    // Calcular coordenadas destino
    double newX = basePos.getX() + offsetX;
    double newZ = basePos.getZ() + offsetZ;
    int topY = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
      (int) newX, (int) newZ);

    // Evita alturas fuera de rango
    if (topY <= world.getBottomY() || topY >= world.getTopY()) return;

    // Teletransportar al Pokémon
    targetPokemon.teleport(newX + 0.5, topY, newZ + 0.5, false);
  }


}
