package com.kingpixel.cobbleraids.managers;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.model.PokemonRaid;
import com.kingpixel.cobbleraids.model.Raid;
import com.kingpixel.cobbleraids.rewards.LastHitRewards;
import com.kingpixel.cobbleraids.rewards.RaidRewards;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import kotlin.Unit;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 17/01/2025 22:47
 */
@Getter
@Setter
@ToString
public class BattleManager {
  private Raid raid;
  private PokemonRaid pokemonRaid;
  private PokemonEntity raidEntity;
  private BossBar bossBar;
  private UUID raidUUID;
  private Map<UUID, Integer> damageMap;
  private int maxLife;
  private int currentLife;
  private Date finishTime;
  private List<PokemonEntity> fakePokemons;
  private ServerPlayerEntity lastHit;

  public BattleManager(Raid raid) {
    this.raid = raid;
    this.pokemonRaid = raid.getPokemonRaid();
    this.raidEntity = pokemonRaid.genPokemonEntity(raid);
    if (raidEntity == null) return;
    raidUUID = raidEntity.getUuid();
    damageMap = new HashMap<>();
    if (CobbleRaids.config.isMoreHealthByEachPlayer()) {
      maxLife = pokemonRaid.getLife() * CobbleRaids.server.getPlayerManager().getPlayerList().size();
    } else {
      maxLife = pokemonRaid.getLife();
    }
    currentLife = maxLife;
    finishTime = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(raid.getTime()));
    fakePokemons = new ArrayList<>();
  }

  private void sendBossBar() {
    var title = AdventureTranslator.toNative(
      pokemonRaid.getBossBar().getTitle()
        .replace("%boss%", raid.getName())
        .replace("%pokemon%", raidEntity == null ? "" : raidEntity.getPokemon().showdownId())
        .replace("%hp%", String.valueOf(currentLife))
        .replace("%hp_max%", String.valueOf(maxLife))
    );
    if (bossBar == null) {
      bossBar =
        new ServerBossBar(title,
          pokemonRaid.getBossBar().getColor(),
          pokemonRaid.getBossBar().getStyle());
    }
    bossBar.setName(title);
    bossBar.setPercent((float) CobbleRaids.battleManager.getCurrentLife() / CobbleRaids.battleManager.getMaxLife());
    BossBarS2CPacket packet = BossBarS2CPacket.add(bossBar);
    CobbleRaids.server.getPlayerManager().sendToAll(packet);
  }

  public static void startRaid(@Nullable Raid raid) {
    try {
      if (CobbleRaids.battleManager != null) CobbleRaids.battleManager.finishRaid(false);
      CobbleRaids.startDate = null;
      PlayerUtils.sendMessage(
        null,
        CobbleRaids.language.getMessageStartRaid(),
        CobbleRaids.config.getPrefix(),
        TypeMessage.BROADCAST
      );
      boolean isActive = false;
      while (!isActive) {
        if (raid == null) raid = CobbleRaids.raidsConfig.getRandomRaid();
        isActive = raid.isActive();
        if (isActive) {
          CobbleRaids.battleManager = new BattleManager(raid);
          CobbleRaids.battleManager.sendBossBar();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void giveRewards() {
    for (RaidRewards reward : raid.getRewards()) {
      if (reward instanceof LastHitRewards killRewards) {
        killRewards.giveRewards(lastHit);
      } else {
        reward.giveRewards(damageMap);
      }
    }
  }

  public void finishRaid(boolean defeat) {
    BossBarS2CPacket packet = BossBarS2CPacket.remove(bossBar.getUuid());
    CobbleRaids.server.getPlayerManager().sendToAll(packet);

    if (raid.isNeedDefeat() && defeat) {
      giveRewards();
    } else if (!raid.isNeedDefeat()) {
      giveRewards();
    }

    CobbleRaids.startDate = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(CobbleRaids.config.getCooldown()));
    PlayerUtils.sendMessage(null, CobbleRaids.language.getMessageFinishRaid(), CobbleRaids.config.getPrefix(),
      TypeMessage.BROADCAST);

    for (PokemonEntity fakePokemon : fakePokemons) {
      if (fakePokemon == null) continue;
      UUID battleUUID = fakePokemon.getBattleId();
      if (battleUUID != null) {
        var battle = Cobblemon.INSTANCE.getBattleRegistry().getBattle(battleUUID);
        if (battle != null) {
          battle.end();
        }
      }
      fakePokemon.remove(Entity.RemovalReason.DISCARDED);
    }
    List<PokemonEntity> fakePokemons = raidEntity.getEntityWorld().getEntitiesByType(TypeFilter.instanceOf(PokemonEntity.class),
      Box.of(raidEntity.getPos(), 16, 16, 16), entity -> {
        if (entity == null) return false;
        NbtCompound persistentData = entity.getPokemon().getPersistentData();
        return persistentData.getBoolean(CobbleRaids.TAG_FAKERAID) || persistentData.getBoolean(CobbleRaids.TAG_RAID);
      });
    for (PokemonEntity fakePokemon : fakePokemons) {
      fakePokemon.remove(Entity.RemovalReason.DISCARDED);
    }
    sendInfo();
    CobbleRaids.battleManager = null;
  }

  public void startBattle(ServerPlayerEntity player) {

    try {
      var party = Cobblemon.INSTANCE.getStorage().getParty(player);
      UUID pokemonUUID = null;
      for (Pokemon pokemon : party) {
        if (!pokemon.isFainted()) {
          pokemonUUID = pokemon.getUuid();
          break;
        }
      }
      Pokemon pokemon = PokemonProperties.Companion.parse(pokemonRaid.getPokemon() + " uncatchable=yes").create();
      pokemon.getPersistentData().putBoolean(CobbleRaids.TAG_FAKERAID, true);
      pokemon.setScaleModifier(0.1f);
      pokemonRaid.apply(pokemon);
      PokemonEntity fakePokemon = pokemon
        .sendOut((ServerWorld) raidEntity.getEntityWorld(), raidEntity.getPos(), null,
          pokemonEntity1 -> Unit.INSTANCE);
      if (fakePokemon == null) {
        CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "Pokemon not found: " + fakePokemon);
        return;
      }
      fakePokemon.setPersistent();
      fakePokemon.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, -1, 9999, false, false));
      fakePokemon.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, -1, 9999, false, false));
      fakePokemon.setNoGravity(true);
      fakePokemon.setAiDisabled(true);
      fakePokemon.setMovementSpeed(0);
      fakePokemon.setCustomName(AdventureTranslator.toNative(raid.getName()));
      BattleBuilder.INSTANCE.pve(player,
        fakePokemon,
        pokemonUUID,
        BattleFormat.Companion.getGEN_9_SINGLES(),
        false,
        raid.isHeal(),
        Cobblemon.config.getDefaultFleeDistance(),
        Cobblemon.INSTANCE.getStorage().getParty(player)
      );
      fakePokemons.add(fakePokemon);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void removeLife(ServerPlayerEntity player, Pokemon pokemon) {
    if (pokemon == null) return;
    lastHit = player;
    int live = pokemon.getCurrentHealth();
    int maxLife = pokemon.getMaxHealth();
    if (CobbleRaids.config.isDebug()) {
      CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID,
        "Health: " + live + " MaxHealth: " + maxLife);
    }
    int remove = maxLife - live;
    damageMap.compute(player.getUuid(), (k, v) -> v == null ? remove : v + remove);
    currentLife -= remove;
    if (currentLife <= 0) {
      finishRaid(true);
    } else {
      sendBossBar();
    }
  }

  private void sendInfo() {
    String lastHit = this.lastHit == null ? "No one" : this.lastHit.getGameProfile().getName();
    String formatTable = CobbleRaids.language.getFormatTableDamage();
    // Ordenar el mapa por daño
    damageMap = damageMap.entrySet().stream()
      .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
      .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);

    String tableDamage = damageMap.entrySet().stream()
      .map(entry -> {
        var player = CobbleRaids.server.getPlayerManager().getPlayer(entry.getKey());
        String playerName = player == null ? "No one" : player.getGameProfile().getName();
        return formatTable
          .replace("%player%", playerName)
          .replace("%damage%", String.valueOf(entry.getValue()));
      })
      .reduce((s1, s2) -> s1 + "\n" + s2)
      .orElse("");
    PlayerUtils.sendMessage(null, CobbleRaids.language.getMessageLastHit()
        .replace("%player%", lastHit), CobbleRaids.config.getPrefix(),
      TypeMessage.BROADCAST);
    PlayerUtils.sendMessage(null, CobbleRaids.language.getMessageTableDamage()
        .replace("%table%", tableDamage), CobbleRaids.config.getPrefix(),
      TypeMessage.BROADCAST);
  }
}
