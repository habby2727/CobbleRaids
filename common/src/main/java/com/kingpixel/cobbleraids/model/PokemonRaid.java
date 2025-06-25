package com.kingpixel.cobbleraids.model;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.managers.BattleManager;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.Utils;
import kotlin.Unit;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 17/01/2025 23:04
 */
@Getter
@Setter
@ToString
@Data
public class PokemonRaid {
  private static final List<Stats> STATS = List.of(Stats.HP, Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK,
    Stats.SPECIAL_DEFENCE, Stats.SPEED);
  private String pokemon;
  private Double chance;
  private Integer life;
  private String heldItem;
  private float size;
  private String pokemonCapture;
  private int shinyRate;
  private int minIvs;
  private List<String> moves;
  private BossBarModel bossBar;
  private BlackListRaid banned;

  PokemonRaid() {
    this.pokemon = "pikachu level=70 shiny=true";
    this.pokemonCapture = "pikachu";
    this.shinyRate = 2048;
    this.chance = 0.1;
    this.minIvs = 0;
    this.life = 2000;
    this.size = 5.0f;
    this.heldItem = "cobblemon:leftovers";
    this.bossBar = new BossBarModel();
    this.moves = List.of("thunderbolt", "quickattack", "thunderwave", "irontail");
    this.banned = new BlackListRaid();
  }

  /**
   * Apply the pokemon to the raid
   *
   * @param pokemon the pokemon to apply
   */
  public void apply(Pokemon pokemon) {
    int i = 0;
    for (String move : moves) {
      if (i < 4) {
        MoveTemplate moveTemplate = Moves.INSTANCE.getByName(move);
        if (moveTemplate == null) {
          CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "Move not found: " + move);
          continue;
        }
        Move move1 = moveTemplate.create(3);
        pokemon.getMoveSet().setMove(i, move1);
        i++;
      }
    }
    for (Move move : pokemon.getMoveSet()) {
      move.setCurrentPp(99);
      move.setRaisedPpStages(3);
    }
    if (!this.heldItem.isEmpty()) pokemon.setHeldItem$common(Utils.parseItemId(this.heldItem));
  }

  public static ServerWorld getWorld(Raid raid) {
    ServerWorld serverWorld = null;
    for (ServerWorld world : CobbleRaids.server.getWorlds()) {
      String nameSpace = world.getRegistryKey().getValue().toString();
      if (nameSpace.equals(raid.getWorld())) {
        serverWorld = world;
        break;
      }
    }
    return serverWorld;
  }

  public PokemonEntity genPokemonEntity(Raid raid) {
    ServerWorld serverWorld = getWorld(raid);


    if (serverWorld == null) {
      CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "World not found: " + raid.getWorld());
      return null;
    }
    Vec3d pos = new Vec3d(raid.getPosRaid().x, raid.getPosRaid().y, raid.getPosRaid().z);

    List<PokemonEntity> entities = serverWorld.getEntitiesByClass(PokemonEntity.class, Box.of(pos, 16, 16, 16)
      , entity -> {
        NbtCompound nbt = entity.getPokemon().getPersistentData();
        return nbt.getBoolean(CobbleRaids.TAG_RAID) || nbt.getBoolean(CobbleRaids.TAG_FAKERAID);
      });
    for (PokemonEntity pokemonEntity : entities) {
      NbtCompound nbt = pokemonEntity.getPokemon().getPersistentData();
      if (nbt.getBoolean(CobbleRaids.TAG_RAID) || nbt.getBoolean(CobbleRaids.TAG_FAKERAID)) {
        BattleManager.activeRaids.stream()
          .filter(raidStarted -> raidStarted.getRaidEntity().getUuid().equals(pokemonEntity.getUuid()))
          .findFirst()
          .ifPresent(RaidStarted::finish);
        pokemonEntity.remove(Entity.RemovalReason.DISCARDED);

      }
    }
    Pokemon raidPokemon = PokemonProperties.Companion.parse(pokemon + " uncatchable=yes").create();
    raidPokemon.getPersistentData().putBoolean(CobbleRaids.TAG_RAID, true);
    raidPokemon.getPersistentData().putString(CobbleRaids.TAG_RAID_ID, raid.getId());

    raidPokemon.setScaleModifier(size);
    apply(raidPokemon);
    PokemonEntity pokemonEntity = raidPokemon
      .sendOut(serverWorld, pos, null,
        pokemonEntity1 -> {
          pokemonEntity1.setPersistent();
          pokemonEntity1.teleport(serverWorld, raid.getPosRaid().x, raid.getPosRaid().y,
            raid.getPosRaid().z, PositionFlag.ROT,
            raid.getDirectionX(), raid.getDirectionY());
          pokemonEntity1.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, -1, 9999, true, true));
          pokemonEntity1.setCustomName(AdventureTranslator.toNative(raid.getName()));
          pokemonEntity1.setMovementSpeed(0);
          pokemonEntity1.setNoGravity(true);
          pokemonEntity1.setAiDisabled(true);
          pokemonEntity1.setGlowing(true);
          pokemonEntity1.teleport(raid.getPosRaid().x, raid.getPosRaid().y, raid.getPosRaid().z, false);
          raidPokemon.getPersistentData().putUuid(CobbleRaids.TAG_RAID_ACTIVE, pokemonEntity1.getUuid());
          return Unit.INSTANCE;

        });
    if (pokemonEntity == null) {
      CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "Pokemon not found: " + pokemonEntity);
      return null;
    }
    return pokemonEntity;
  }


  /**
   * Check if the player is permited to join the raid
   *
   * @param player the player to check
   *
   * @return true if the player is permited to join the raid
   */
  public boolean isPermitted(ServerPlayerEntity player) {
    for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getParty(player)) {
      if (CobbleRaids.config.getBanned().isBanned(player, pokemon)) return false;
      if (getBanned().isBanned(player, pokemon)) return false;
    }
    return true;
  }

  @Override protected Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  public Pokemon obtainPokemon() {
    return PokemonProperties.Companion.parse(pokemon.trim()).create();
  }

  public Pokemon obtainPokemonCapture() {
    Pokemon p = PokemonProperties.Companion.parse(pokemonCapture.trim()).create();
    if (shinyRate <= 0 || Utils.RANDOM.nextInt(shinyRate) == 0) p.setShiny(true);
    List<Stats> stats = new ArrayList<>(STATS);
    int size = stats.size();
    for (int i = 0; i < size; i++) {
      Stats stat = stats.remove(Utils.RANDOM.nextInt(stats.size()));
      int iv = Utils.RANDOM.nextInt(minIvs, 32);
      p.getIvs().set(stat, iv);
    }
    return p;
  }
}
