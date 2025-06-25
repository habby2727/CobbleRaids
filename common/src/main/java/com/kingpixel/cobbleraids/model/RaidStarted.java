package com.kingpixel.cobbleraids.model;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.managers.BattleManager;
import com.kingpixel.cobbleraids.rewards.DamageRewards;
import com.kingpixel.cobbleraids.rewards.LastHitRewards;
import com.kingpixel.cobbleraids.rewards.RaidRewards;
import com.kingpixel.cobblesize.Model.SizeChance;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.kingpixel.cobbleutils.util.Utils;
import kotlin.Unit;
import lombok.Data;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 05/02/2025 3:11
 */
@Data
public class RaidStarted {
  private boolean finished;
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
  private ServerWorld world;
  private int chunkX;
  private int chunkZ;


  public RaidStarted(Raid raid, @Nullable PokemonRaid r) {
    this.raid = raid;
    if (r == null) {
      pokemonRaid = raid.getPokemonRaid();
    } else {
      pokemonRaid = r;
    }
    this.raidEntity = pokemonRaid.genPokemonEntity(raid);
    this.raidUUID = raidEntity.getUuid();
    this.damageMap = new ConcurrentHashMap<>();
    this.world = (ServerWorld) raidEntity.getEntityWorld();

    this.chunkX = raidEntity.getBlockPos().getX() >> 4;
    this.chunkZ = raidEntity.getBlockPos().getZ() >> 4;
    world.setChunkForced(chunkX, chunkZ, true);

    if (CobbleRaids.config.isMoreHealthByEachPlayer()) {
      int count = 0;
      for (ServerPlayerEntity player : CobbleRaids.server.getPlayerManager().getPlayerList()) {
        if (player == null) continue;
        if (!PermissionApi.hasPermission(player, "cobbleraids.morehealth.bypass", 2)) {
          count++;
        }
      }
      this.maxLife = pokemonRaid.getLife() * (count == 0 ? 1 : count);
    } else {
      this.maxLife = pokemonRaid.getLife();
    }
    this.currentLife = maxLife;
    this.finishTime = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(raid.getTime()));
    this.fakePokemons = new ArrayList<>();
  }


  public void startBattle(ServerPlayerEntity player) {
    if (player == null) return;
    Pokemon pokemon = raidEntity.getPokemon().clone(true, DynamicRegistryManager.EMPTY);

    pokemon.setShiny(false);
    if (CobbleRaids.config.isDebug()) {
      pokemon.setScaleModifier(0.5f);
    } else {
      pokemon.setScaleModifier(0.01f);
    }

    pokemonRaid.apply(pokemon);
    pokemon.getPersistentData().remove(CobbleRaids.TAG_RAID);
    pokemon.getPersistentData().putBoolean(CobbleRaids.TAG_FAKERAID, true);
    pokemon.getPersistentData().putUuid(CobbleRaids.TAG_RAID_ACTIVE, raidEntity.getUuid());

    var party = Cobblemon.INSTANCE.getStorage().getParty(player);

    int count = 0;
    int totalLevel = 0;
    boolean found = false;
    UUID pokemonUUID = null;
    for (Pokemon pokemonParty : party) {
      changeSize(pokemonParty);
      count++;
      totalLevel += pokemonParty.getLevel();
      if (!pokemonParty.isFainted() && !found) {
        pokemonUUID = pokemonParty.getUuid();
        found = true;
      }
    }

    if (pokemonUUID == null) return;

    if (count > 0) {
      int avgLevel = totalLevel / count;
      if (pokemon.getLevel() < avgLevel) {
        pokemon.setLevel(avgLevel);
      }
    }
    var pos = raidEntity.getPos();
    // Randomizame en 2 la pos
    pos = pos.add(
      Utils.RANDOM.nextInt(3) - 1,
      0,
      Utils.RANDOM.nextInt(3) - 1
    );
    PokemonEntity fakePokemon = pokemon.sendOut((ServerWorld) raidEntity.getEntityWorld(), pos, null, entity -> {
      if (!CobbleRaids.config.isDebug()) {
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, -1, 9999, false, false));
      }
      entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, -1, 9999, false, false));
      entity.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, -1, 9999, false, false));
      entity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, -1, 9999, false, false));
      entity.setNoGravity(true);
      entity.setAiDisabled(true);
      entity.setMovementSpeed(0);
      entity.setCustomName(AdventureTranslator.toNative(raid.getName()));
      return Unit.INSTANCE;
    });


    if (fakePokemon == null) return;

    Cobblemon.INSTANCE.getConfig().setMaxPokemonLevel(999);
    fakePokemon.getPokemon().setLevel(pokemon.getLevel() + CobbleRaids.config.getOverLevel());
    Cobblemon.INSTANCE.getConfig().setMaxPokemonLevel(CobbleRaids.oldLevelCap);


    var battle = BattleBuilder.INSTANCE.pve(player,
      fakePokemon,
      pokemonUUID,
      BattleFormat.Companion.getGEN_9_SINGLES(),
      false,
      raid.isHeal(),
      Cobblemon.config.getDefaultFleeDistance(),
      Cobblemon.INSTANCE.getStorage().getParty(player)
    );


    fakePokemons.add(fakePokemon);
  }

  public void finishBattle(ServerPlayerEntity player, Pokemon pokemon) {
    if (player == null) return;
    var pokemonEntity = fakePokemons.stream()
      .filter(pokemonEntity1 -> pokemonEntity1.getPokemon().getUuid().equals(pokemon.getUuid()))
      .findFirst()
      .orElse(null);
    if (pokemonEntity == null) return;
    pokemonEntity.teleport(pokemonEntity.getX(), -1000, pokemonEntity.getZ(), false);
    fakePokemons.remove(pokemonEntity);
    resolverSize(player);
    removeLife(player, pokemonEntity);
  }


  private void resolverSize(ServerPlayerEntity player) {
    try {
      Cobblemon.INSTANCE.getStorage().getParty(player).forEach(SizeChance::solveSize);
    } catch (NoClassDefFoundError | NoSuchMethodError | Exception ignored) {
    }
  }

  public void removeLife(ServerPlayerEntity player, PokemonEntity pokemonEntity) {
    Pokemon pokemon = pokemonEntity.getPokemon();
    if (player == null) {
      CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "Player is null");
      return;
    }
    lastHit = player;
    int live = pokemon.getCurrentHealth();
    int maxLife = pokemon.getMaxHealth();
    int remove = maxLife - live;
    damageMap.compute(player.getUuid(), (k, v) -> (v == null ? 0 : v) + remove);
    currentLife -= remove;
    if (currentLife <= 0) {
      finish();
    } else {
      sendBossBar();
    }
    pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
  }

  public void sendBossBar() {
    // BossBar
    var title = AdventureTranslator.toNative(
      pokemonRaid.getBossBar().getTitle()
        .replace("%boss%", raid.getName())
        .replace("%pokemon%", raidEntity == null ? "" : raidEntity.getPokemon().showdownId())
        .replace("%hp%", String.valueOf(currentLife))
        .replace("%hp_max%", String.valueOf(maxLife))
    );
    BossBarS2CPacket packetBossBar = null;
    if (pokemonRaid.getBossBar() != null) {
      if (bossBar == null) {
        bossBar =
          new ServerBossBar(title,
            pokemonRaid.getBossBar().getColor(),
            pokemonRaid.getBossBar().getStyle());
      }
      bossBar.setName(title);
      bossBar.setPercent((float) getCurrentLife() / getMaxLife());
      packetBossBar = BossBarS2CPacket.add(bossBar);
    }

    // TODO: ScoreBoard

    var players = raidEntity.getEntityWorld().getEntitiesByClass(ServerPlayerEntity.class,
      raidEntity.getBoundingBox().expand(64), player -> true);

    for (ServerPlayerEntity player : CobbleRaids.server.getPlayerManager().getPlayerList()) {
      if (player == null) continue;
      if (packetBossBar != null) {
        if (players.contains(player)) {
          player.networkHandler.sendPacket(packetBossBar);
        } else {
          player.networkHandler.sendPacket(BossBarS2CPacket.remove(bossBar.getUuid()));
        }
      }
    }
  }

  public void finish() {
    finished = true;
    raidEntity.remove(Entity.RemovalReason.DISCARDED);
    for (PokemonEntity fakePokemon : fakePokemons) {
      var battle = Cobblemon.INSTANCE.getBattleRegistry().getBattle(fakePokemon.getUuid());
      if (battle != null) battle.end();
      fakePokemon.remove(Entity.RemovalReason.DISCARDED);
    }
    for (ServerPlayerEntity player : CobbleRaids.server.getPlayerManager().getPlayerList()) {
      if (player == null) continue;
      resolverSize(player);
      player.networkHandler.sendPacket(BossBarS2CPacket.remove(bossBar.getUuid()));
    }
    if (raid.getType().equals(TypeRaid.GLOBAL)) {
      CobbleRaids.startDate =
        new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(CobbleRaids.config.getCooldown()));
    }
    sendInfo();
    giveRewards();
    fakePokemons.clear();

    world.setChunkForced(chunkX, chunkZ, false);

    BattleManager.activeRaids.remove(this);
  }


  private void giveRewards() {
    if (raid.isNeedDefeat()) {
      if (currentLife > 0) return;
    }
    for (RaidRewards reward : raid.getRewards()) {
      if (reward instanceof LastHitRewards killRewards) {
        if (lastHit == null) return;
        killRewards.giveRewards(lastHit);
        lastHit.addStatusEffect(new StatusEffectInstance(
          StatusEffects.GLOWING, 20 * 15, 1, true, false, false
        ));

      } else if (reward instanceof DamageRewards damageRewards) {
        damageRewards.giveRewards(damageMap, pokemonRaid);
      } else {
        reward.giveRewards(damageMap);
      }
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

  private boolean hasCobbleSize = false;

  private void changeSize(Pokemon pokemon) {
    String size = pokemon.getPersistentData().getString("size");
    if (size.isEmpty() || size.equals("custom")) return;
    try {
      SizeChance.getSizes(pokemon);
      pokemon.setScaleModifier(0.1f);
      if (CobbleRaids.config.isDebug()) {
        CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "Change size");
      }
    } catch (NoClassDefFoundError | NoSuchMethodError e) {
      if (CobbleRaids.config.isDebug()) {
        CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "CobbleSize not found");
      }
      return;
    }
  }
}
