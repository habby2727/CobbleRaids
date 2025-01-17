package com.kingpixel.cobbleraids.managers;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.model.PokemonRaid;
import com.kingpixel.cobbleraids.model.Raid;
import com.kingpixel.cobbleraids.rewards.RaidRewards;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 17/01/2025 22:47
 */
@Getter
@Setter
public class BattleManager {
  private Raid raid;
  private PokemonRaid pokemonRaid;
  private PokemonEntity pokemonEntity;
  private UUID raidUUID;
  private Map<UUID, Integer> damageMap;
  private int maxLife;
  private int currentLife;
  private long finishTime;

  public BattleManager(Raid raid) {
    this.raid = raid;
    this.pokemonRaid = raid.getPokemonRaid();
    this.pokemonEntity = pokemonRaid.genPokemonEntity(raid);
    if (pokemonEntity == null) return;
    raidUUID = pokemonEntity.getUuid();
    damageMap = new HashMap<>();
    maxLife = pokemonRaid.getLife();
    currentLife = maxLife;
    finishTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(raid.getTime());
  }

  public static void startRaid() {
    CobbleRaids.battleManager = new BattleManager(CobbleRaids.raidsConfig.getRandomRaid());

  }

  public void finishRaid() {
    for (RaidRewards reward : raid.getRewards()) {
      reward.giveRewards(damageMap);
    }
    PlayerUtils.sendMessage(null, "Raid finished!", CobbleRaids.config.getPrefix(), TypeMessage.BROADCAST);
    pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
    CobbleRaids.battleManager = null;
  }
}
