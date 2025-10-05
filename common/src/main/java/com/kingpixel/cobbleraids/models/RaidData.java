package com.kingpixel.cobbleraids.models;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import lombok.Data;
import net.minecraft.item.ItemStack;

import java.util.*;

/**
 * @author Carlos Varas Alonso - 13/09/2025 2:28
 */
@Data
public class RaidData {
  private String id;
  private final String category;
  private double chance;
  private final String capturePokemon;
  transient
  private Pokemon capturePokemonInstance;
  private final String pokemon;
  private final NavigableMap<Double, String> pokemonPhases = new TreeMap<>(Comparator.reverseOrder());
  // TODO: States raid: Shield, Health regen, Weather, Terrain, Status... <Range health percentage, State>

  public RaidData() {
    this.id = "default";
    this.category = "default";
    this.chance = 100;
    this.capturePokemon = "pikachu";
    this.pokemon = "pikachu";
  }

  public RaidData(String id, String category) {
    this.id = id;
    this.category = category;
    this.chance = 100;
    this.capturePokemon = "pikachu";
    this.pokemon = "pikachu";
  }

  public Pokemon getCapturePokemonInstance() {
    if (capturePokemonInstance == null) {
      capturePokemonInstance = PokemonProperties.Companion.parse(capturePokemon).create();
    }
    return capturePokemonInstance;
  }

  public CategoryRaid getCategoryRaid() {
    return CobbleRaids.categorys.getCategory(category);
  }


  public String getActualPhase(Raid raid) {
    if (pokemonPhases.isEmpty()) return pokemon;

    double currentHealth = raid.getHealth();
    double maxHealth = raid.getMaxHealth();
    double healthPercentage = (currentHealth / maxHealth) * 100.0;

    // Buscar la primera fase cuyo umbral sea >= vida actual
    for (Map.Entry<Double, String> entry : pokemonPhases.entrySet()) {
      if (healthPercentage <= entry.getKey()) {
        return entry.getValue();
      }
    }

    // Si no entra en ningún rango, devolvemos la forma base
    return pokemon;
  }


  public boolean isBannedPokemon(Pokemon pokemon) {
    if (getCategoryRaid().isBlackList(pokemon)) return true;
    return CobbleRaids.config.getBlacklist().isBanned(pokemon);
  }

  public boolean isBannedPlayer(String playerUUID) {
    return false;
  }

  transient
  private Map<String, ItemStack> cacheDisplay = new HashMap<>();

  public ItemStack getActualPhasePokemonItem(Raid raid) {
    String result = getActualPhase(raid);
    ItemStack display = cacheDisplay.get(result);
    if (display != null) return display;
    Pokemon pokemon = PokemonProperties.Companion.parse(result).create();
    display = PokemonItem.from(pokemon);
    cacheDisplay.put(result, display);
    return display;
  }
}
