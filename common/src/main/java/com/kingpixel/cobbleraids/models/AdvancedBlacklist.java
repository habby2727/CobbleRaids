package com.kingpixel.cobbleraids.models;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.Model.PokemonBlackList;
import lombok.Data;

import java.util.Set;

/**
 * @author Carlos Varas Alonso - 13/09/2025 2:23
 */
@Data
public class AdvancedBlacklist {
  private PokemonBlackList pokemonBlackList;
  private Set<String> moves;
  private Set<String> abilities;
  private Set<String> items;

  public AdvancedBlacklist() {
    pokemonBlackList = new PokemonBlackList();
    moves = Set.of();
    abilities = Set.of();
    items = Set.of();
  }

  public boolean isBanned(Pokemon pokemon) {
    if (pokemon == null) return false;
    if (pokemonBlackList.isBlackListed(pokemon)) return true;
    var moveSet = pokemon.getMoveSet();
    for (Move move : moveSet) {
      if (moves.contains(move.getName())) return true;
    }
    if (abilities.contains(pokemon.getAbility().getName())) return true;
    var heldItem = pokemon.heldItem();
    if (heldItem.isEmpty()) return false;
    return items.contains(heldItem.getItem().toString());
  }
}
