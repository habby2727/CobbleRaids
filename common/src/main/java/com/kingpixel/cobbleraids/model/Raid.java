package com.kingpixel.cobbleraids.model;

import com.kingpixel.cobbleraids.config.RaidsConfig;
import com.kingpixel.cobbleraids.rewards.RaidRewards;
import com.kingpixel.cobbleutils.Model.Sound;
import lombok.Data;
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
@Data
public class Raid {
  private boolean active;
  private TypeRaid type;
  private boolean heal;
  private boolean needDefeat;
  private String id;
  private String rewardId;
  private String name;
  private double chance;
  private int time;
  private String world;
  private Vector3d posPlayer;
  private Vector3d posRaid;
  private float directionX;
  private float directionY;
  private Sound sound;
  private List<PokemonRaid> pokemons;
  private List<RaidRewards> rewards;


  public Raid() {
    this.active = true;
    this.type = TypeRaid.GLOBAL;
    this.heal = true;
    this.needDefeat = true;
    this.id = "default";
    this.rewardId = "default";
    this.name = "&cdefault";
    this.chance = 20.0;
    this.time = 5;
    this.sound = new Sound();
    this.world = "overworld";
    this.posRaid = new Vector3d(1, 1, 1);
    this.directionX = 0;
    this.directionY = 0;
    this.posPlayer = new Vector3d(1, 1, 1);
    this.pokemons = new ArrayList<>();
    pokemons.add(new PokemonRaid());
    pokemons.add(new PokemonRaid());

  }

  public static Raid getRaid(String raid) {
    for (List<Raid> list : RaidsConfig.raids.values()) {
      for (Raid r : list) {
        if (r.getId().equals(raid)) {
          return r;
        }
      }
    }
    return null;
  }

  public void check() {
    for (PokemonRaid pokemon : pokemons) {
      if (pokemon.getBossBar() == null) pokemon.setBossBar(new BossBarModel());
    }
    if (!world.contains(":")) {
      world = "minecraft:" + world;
    }
    if (sound == null) sound = new Sound();
    if (rewardId == null) rewardId = id;
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
    return pokemons.getFirst();
  }

  @Override protected Object clone() throws CloneNotSupportedException {
    return super.clone();
  }
}
