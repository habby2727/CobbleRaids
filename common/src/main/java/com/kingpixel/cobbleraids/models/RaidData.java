package com.kingpixel.cobbleraids.models;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import lombok.Data;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Carlos Varas Alonso - 13/09/2025 2:28
 */
@Data
public class RaidData {
  private String id;
  private final String category;
  private final String pokemon;
  private final Map<Double, String> pokemonPhases;

  public RaidData(String id, String category) {
    this.id = id;
    this.category = category;
    this.pokemon = "pikachu";
    this.pokemonPhases = new HashMap<>();
  }

  public CategoryRaid getCategoryRaid() {
    return CobbleRaids.categorys.getCategory(category);
  }

  public String getActualPhase(Raid raid) {
    var currentHealth = raid.getHealth();
    var maxHealth = raid.getMaxHealth();
    var healthPercentage = (currentHealth / maxHealth) * 100;
    if (pokemonPhases.isEmpty()) return pokemon;

    String result = pokemon;
    var entries = pokemonPhases.entrySet();
    for (Map.Entry<Double, String> doubleStringEntry : entries) {
      if (healthPercentage <= doubleStringEntry.getKey()) {
        result = doubleStringEntry.getValue();
      }
    }
    return result;
  }

  public boolean isBannedPokemon(Pokemon pokemon) {
    return false;
  }

  public boolean isBannedPlayer(String playerUUID) {
    return false;
  }

  transient
  private Map<String, ItemStack> cacheDisplay = new HashMap<>();

  public ItemStack getActualPhasePokemonItem(Raid raid) {
    if (cacheDisplay == null) {
      cacheDisplay = new HashMap<>();
    }
    String result = getActualPhase(raid);
    ItemStack display = cacheDisplay.get(result);
    if (display != null) return display;
    Pokemon pokemon = PokemonProperties.Companion.parse(result).create();
    display = PokemonItem.from(pokemon);
    cacheDisplay.put(result, display);
    return display;
  }
}
