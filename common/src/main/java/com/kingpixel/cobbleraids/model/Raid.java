package com.kingpixel.cobbleraids.model;

import com.kingpixel.cobbleraids.rewards.RaidRewards;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 17/01/2025 0:26
 */
@Getter
@Setter
@ToString
public class Raid {
  private boolean active;
  private boolean heal;
  private String id;
  private String name;
  private double chance;
  private int time;
  private String world;
  private Vector3d posRaid;
  private Vector3d posPlayer;
  private BossBarModel bossBar;
  private List<PokemonRaid> pokemons;
  private List<RaidRewards> rewards;


  public Raid() {
    this.active = true;
    this.heal = true;
    this.id = "default";
    this.name = "&cdefault";
    this.chance = 20.0;
    this.time = 5;
    this.world = "overworld";
    this.posRaid = new Vector3d(1, 1, 1);
    this.posPlayer = new Vector3d(1, 1, 1);
    this.bossBar = new BossBarModel();
    this.pokemons = new ArrayList<>();
    pokemons.add(new PokemonRaid());

  }

  public void check() {

  }

  public PokemonRaid getPokemonRaid() {
    double totalWeight = pokemons.stream().mapToDouble(PokemonRaid::getChance).sum();
    double random = Math.random() * totalWeight;
    for (PokemonRaid pokemon : pokemons) {
      random -= pokemon.getChance();
      if (random <= 0) {
        return pokemon;
      }
    }
    return null;
  }
}
