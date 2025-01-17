package com.kingpixel.cobbleraids.model;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 17/01/2025 23:04
 */
@Getter
@Setter
public class PokemonRaid {
  private String pokemon;
  private Double chance;
  private Integer life;
  private float size;
  private List<String> pokemonBlackList;
  private List<String> abilityBlackList;
  private List<String> moveBlackList;
  private List<String> itemBlackList;

  PokemonRaid() {
    this.pokemon = "pikachu";
    this.chance = 0.1;
    this.life = 10000;
    this.size = 1.0f;
    this.pokemonBlackList = List.of("pikachu", "magikarp");
    this.abilityBlackList = List.of("ability1", "ability2");
    this.moveBlackList = List.of("move1", "move2");
    this.itemBlackList = List.of("item1", "item2");
  }


  public PokemonEntity genPokemonEntity(Raid raid) {
    ServerWorld serverWorld = null;
    for (ServerWorld world : CobbleRaids.server.getWorlds()) {
      String nameSpace = world.getRegistryKey().getValue().getNamespace();
      if (CobbleRaids.config.isDebug()) {
        CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "World: " + nameSpace);
      }
      if (nameSpace.equals(raid.getWorld())) {
        serverWorld = world;
        break;
      }
    }
    if (serverWorld == null) {
      CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "World not found: " + raid.getWorld());
      return null;
    }
    PokemonEntity pokemonEntity = PokemonProperties.Companion.parse(pokemon + " uncatchable=yes").createEntity(serverWorld);
    Pokemon pokemon = pokemonEntity.getPokemon();

    pokemon.setScaleModifier(size);
    return pokemonEntity;
  }

  /**
   * Check if the player is permited to join the raid
   *
   * @param player the player to check
   *
   * @return true if the player is permited to join the raid
   */
  public boolean isPermited(ServerPlayerEntity player) {
    boolean isPermited = true;
    for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getParty(player)) {
      if (CobbleRaids.config.getGlobalPokemonBlackList().contains(pokemon.showdownId())) return false;
      if (CobbleRaids.config.getGlobalAbilitiesBlackList().contains(pokemon.getAbility().getName())) return false;
      for (Move move : pokemon.getMoveSet()) {
        if (CobbleRaids.config.getGlobalMovesBlackList().contains(move.getName())) return false;
      }
      if (CobbleRaids.config.getGlobalItemsBlackList().contains(pokemon.getHeldItem$common().getTranslationKey().replace("item.", "")))
        return false;
      if (pokemonBlackList.contains(pokemon.showdownId())) return false;
      if (abilityBlackList.contains(pokemon.getAbility().getName())) return false;
      for (Move move : pokemon.getMoveSet()) {
        if (moveBlackList.contains(move.getName())) return false;
      }
      if (itemBlackList.contains(pokemon.getHeldItem$common().getTranslationKey().replace("item.", ""))) return false;
    }
    return isPermited;
  }
}
