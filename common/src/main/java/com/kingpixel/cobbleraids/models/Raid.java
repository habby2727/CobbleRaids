package com.kingpixel.cobbleraids.models;

import ca.landonjw.gooeylibs2.api.UIManager;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.database.DataBaseFactory;
import com.kingpixel.cobbleraids.events.RaidEvents;
import com.kingpixel.cobbleraids.events.models.RaidFinished;
import com.kingpixel.cobbleraids.events.models.RaidNewPhase;
import com.kingpixel.cobbleraids.events.models.RaidPostStarted;
import com.kingpixel.cobbleraids.events.models.RaidPreStarted;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import kotlin.Unit;
import lombok.Data;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.*;

/**
 * @author Carlos Varas Alonso - 13/09/2025 2:28
 */
@Data
public class Raid {
  public static final String RAID_NBT_KEY = "raid_uuid";
  public static final String RAID_CATEGORY_NBT_KEY = "cat_id";
  private boolean finish;
  private long startTime;
  private long endTime;
  private UUID raidUUID;
  transient
  private Pokemon pokemon;
  transient
  private PokemonEntity raidEntity;
  transient
  private RaidData raidData;
  transient
  private CategoryRaid categoryRaid;
  private int health;
  private int maxHealth;
  private Map<UUID, Integer> damageMap;
  transient
  private ServerBossBar bossBar;
  private List<UUID> playersInitCaptureSession = new ArrayList<>();


  public Raid(RaidData raidData, long startTime) {
    this.finish = false;
    this.startTime = System.currentTimeMillis() + startTime;
    this.endTime = this.startTime + raidData.getCategoryRaid().getDurationRaid().toMillis();
    this.raidUUID = UUID.randomUUID();
    this.raidData = raidData;
    this.categoryRaid = raidData.getCategoryRaid();
    this.health = categoryRaid.getHealth();
    this.maxHealth = categoryRaid.getHealth();
    this.pokemon = PokemonProperties.Companion.parse(raidData.getActualPhase(this)).create();
    this.damageMap = new HashMap<>();
    RaidEvents.RAID_STARTED_PRE.emit(new RaidPreStarted(this));
  }

  // Start Raid
  public Raid(RaidData raidData) {
    this.finish = false;
    this.startTime = System.currentTimeMillis();
    this.endTime = this.startTime + raidData.getCategoryRaid().getDurationRaid().toMillis();
    this.raidUUID = UUID.randomUUID();
    this.raidData = raidData;
    this.categoryRaid = raidData.getCategoryRaid();
    this.health = categoryRaid.getHealth();
    this.maxHealth = categoryRaid.getHealth();
    this.damageMap = new HashMap<>();
    this.raidEntity = generateRaidEntity(false);
    this.pokemon = raidEntity.getPokemon();

  }

  public String replace(String text) {
    return PokemonUtils.replace(text, getPokemon());
  }

  public ServerBossBar getBossBar() {
    if (bossBar == null) {
      bossBar = new ServerBossBar(
        AdventureTranslator.toNative(
          PokemonUtils.replace(
            categoryRaid.getBossBarName()
              .replace("%health%", String.valueOf(health))
              .replace("%maxhealth%", String.valueOf(maxHealth)),
            getPokemon()
          ),
          CobbleRaids.language.getPrefix()
        ),
        categoryRaid.getBossBarColor(),
        categoryRaid.getBossBarStyle()
      );
    }
    return bossBar;
  }

  transient
  private int previousHealth = -1;
  transient
  private int previousMaxHealth = -1;
  transient
  private String previousPhase = "";

  public Pokemon getPokemon() {
    return raidEntity == null ? this.pokemon : raidEntity.getPokemon();
  }

  public void refreshBossBar() {
    if (previousHealth != health || previousMaxHealth != maxHealth || !previousPhase.equals(raidData.getActualPhase(this))) {
      previousHealth = health;
      previousMaxHealth = maxHealth;
      bossBar.setName(AdventureTranslator.toNative(
          PokemonUtils.replace(
            categoryRaid.getBossBarName()
              .replace("%health%", String.valueOf(health))
              .replace("%maxhealth%", String.valueOf(maxHealth)),
            getPokemon()
          )
        )
      );
      bossBar.setPercent(Math.max(0f, Math.min(1f, (float) health / maxHealth)));
      glowing();
    }
  }

  private String actualPhase = "";

  private void refreshRaidEntity() {
    try {
      var phase = raidData.getActualPhase(this);
      if (phase.equals(actualPhase)) return;

      actualPhase = phase;
      Pokemon p = PokemonProperties.Companion.parse(actualPhase).create();
      pokemon = p;
      Pokemon pR = raidEntity.getPokemon();
      pR.setSpecies(p.getSpecies());
      pR.setForm(p.getForm());
      pR.setGender(p.getGender());
      pR.setShiny(p.getShiny());
      pR.setForcedAspects(p.getAspects());
      CobbleRaids.server.execute(() -> {
        raidEntity.getPokemon().updateAspects();
        glowing();
      });
      RaidEvents.RAID_NEW_PHASE.emit(new RaidNewPhase(this));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void startBattle(ServerPlayerEntity player) {
    if (finish || health <= 0) return;
    UserInfo userInfo = DataBaseFactory.INSTANCE.findUserByPlayer(player);
    if (categoryRaid.isNeedTicket() && !damageMap.containsKey(player.getUuid())) {
      if (!userInfo.hasTicket(categoryRaid)) {
        PlayerUtils.sendMessage(
          player,
          "§c[§6CobbleRaids§c] §cYou don't have a ticket to join this raid!§r",
          CobbleRaids.language.getPrefix(),
          TypeMessage.CHAT
        );
        return;
      }
    }

    var party = Cobblemon.INSTANCE.getStorage().getParty(player);
    Pokemon leader = null;
    for (Pokemon pokemon : party) {
      if (pokemon != null && !raidData.isBannedPokemon(pokemon)) {
        leader = pokemon;
        break;
      }
    }
    if (leader == null) return;

    PokemonEntity raidEntity = generateRaidEntity(true);
    raidEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, Integer.MAX_VALUE, 255));
    if (!CobbleRaids.config.isDebug()) {
      raidEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, Integer.MAX_VALUE, 255));
    }

    Pokemon finalLeader = leader;

    var fightData = CobbleRaids.raidManager.getFightingPlayer(player.getUuid());
    if (fightData != null) fightData.stop();

    var startBattle = BattleBuilder.INSTANCE.pve(
      player,
      raidEntity,
      finalLeader.getUuid(),
      BattleFormat.Companion.getGEN_9_SINGLES(),
      true,
      true,
      Cobblemon.INSTANCE.getConfig().getDefaultFleeDistance(),
      party
    );
    startBattle.ifSuccessful(pokemonBattle -> {
      var actor = (PlayerBattleActor) pokemonBattle.getActor(player);
      if (actor == null) {
        CobbleUtils.LOGGER.fatal("Could not find actor for player " + player.getName().getString() + " in battle " + pokemonBattle.getBattleId());
        return Unit.INSTANCE;
      }
      CobbleRaids.raidManager.addFightingData(
        pokemonBattle.getBattleId(),
        new FightData(
          pokemonBattle.getBattleId(),
          this,
          player,
          raidEntity
        )
      );
      PlayerUtils.sendMessage(
        player,
        PokemonUtils.replace(
          CobbleRaids.language.getMessageStartBattle(),
          raidEntity.getPokemon()
        ),
        CobbleRaids.language.getPrefix(),
        TypeMessage.CHAT
      );
      if (!damageMap.isEmpty() && !damageMap.containsKey(player.getUuid())) {
        userInfo.removeTicket(categoryRaid);
        maxHealth += categoryRaid.getHealth();
        health += categoryRaid.getHealth();
        damageMap.put(player.getUuid(), 0);
      }
      teleportPlayerOut(player);
      return Unit.INSTANCE;
    });
    startBattle.ifErrored(erroredBattleStart -> {
      PlayerUtils.sendMessage(
        player,
        "§c[§6CobbleRaids§c] §cCould not start the battle, please try again later.§r",
        CobbleRaids.language.getPrefix(),
        TypeMessage.CHAT
      );
      raidEntity.discard();
      return Unit.INSTANCE;
    });

    raidEntity.getPokemon().getPersistentData().putString(RAID_CATEGORY_NBT_KEY, categoryRaid.getId());

  }

  private PokemonEntity generateRaidEntity(boolean fight) {
    var result = raidData.getActualPhase(this);
    var properties = PokemonProperties.Companion.parse(result.trim() + " uncatchable=yes");
    var coords = categoryRaid.getCoords();
    var pokemon = properties.create();
    if (fight) {
      pokemon.getPersistentData().putString(RAID_CATEGORY_NBT_KEY, categoryRaid.getId());
      pokemon.getPersistentData().putUuid(RAID_NBT_KEY, raidUUID);
    } else {
      pokemon.getPersistentData().putUuid(RAID_NBT_KEY, raidUUID);
    }
    ServerWorld serverWorld = categoryRaid.getWorld();
    int x = coords.x() >> 4;
    int z = coords.z() >> 4;
    if (!serverWorld.isChunkLoaded(x, z)) {
      serverWorld.getChunkManager().getChunk(x, z, ChunkStatus.FULL, false);
    }
    var pokemonEntity = pokemon.sendOut(
      serverWorld,
      coords.getVec3d(),
      null,
      entity -> {
        categoryRaid.compute(entity, fight);
        if (fight) {
          entity.getPokemon().getPersistentData().putString(RAID_CATEGORY_NBT_KEY, categoryRaid.getId());
        } else {
          entity.getPokemon().getPersistentData().putUuid(RAID_NBT_KEY, raidUUID);
        }
        return Unit.INSTANCE;
      }
    );
    if (pokemonEntity == null) {
      throw new IllegalStateException("Could not create PokemonEntity for raid");
    }
    if (!fight) {
      this.raidEntity = pokemonEntity;
      glowing();
      RaidEvents.RAID_STARTED_POST.emit(new RaidPostStarted(this));
    }
    return pokemonEntity;

  }

  public void teleportPlayerOut(ServerPlayerEntity player) {
    double radio = categoryRaid.getRadio();
    Vec3d coords = categoryRaid.getCoords().getVec3d();
    Vec3d posPlayer = player.getPos();
    if (!posPlayer.isInRange(coords, radio)) return;
    Vec3d direction = posPlayer.subtract(coords).normalize();
    Vec3d newPos = coords.add(direction.multiply(radio + 5));
    CobbleRaids.server.execute(() -> player.teleport(
      categoryRaid.getWorld(),
      newPos.x,
      posPlayer.getY(),
      newPos.z,
      player.getYaw(),
      player.getPitch()
    ));
  }

  public void updateHealth(FightData fightData, int damage) {
    fightData.stop();
    ServerPlayerEntity player = fightData.getPlayer();
    damageMap.compute(player.getUuid(), (k, damageFromMap) -> (damageFromMap == null ? 0 : damageFromMap) + damage);
    removeDamage(damage);
    if (CobbleRaids.config.isDebug()) {
      PlayerUtils.sendMessage(
        player,
        "§e[§6CobbleRaids§e] §eRaid Boss Health: " + health + "/" + maxHealth + " (-" + damage + ")§r",
        CobbleRaids.language.getPrefix(),
        TypeMessage.CHAT
      );
    }
    if (health > 0) {
      refreshRaidEntity();
      startBattle(player);
    } else finishRaid();
  }

  private synchronized void removeDamage(int damage) {
    health -= damage;
  }

  public synchronized void finishRaid() {
    try {
      if (finish) return;
      finish = true;
      boolean killed = health <= 0;
      RaidEvents.RAID_FINISHED.emit(new RaidFinished(this, killed));
      if (bossBar != null) {
        bossBar.clearPlayers();
      }
      CobbleRaids.server.execute(() -> {
        var pos = raidEntity.getChunkPos();
        if (!raidEntity.getWorld().getChunkManager().isChunkLoaded(pos.x, pos.z)) {
          raidEntity.getWorld().getChunkManager().getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
        }
        if (raidEntity != null) raidEntity.remove(Entity.RemovalReason.DISCARDED);
      });
      CobbleRaids.raidManager.removeRaid(raidUUID);
      List<FightData> fights = CobbleRaids.raidManager.getFightingsByRaidUUID(raidUUID);
      for (FightData fight : fights) {
        UIManager.closeUI(fight.getPlayer());
        fight.stop();
      }
      if (killed) {
        CobbleRaids.rewardsManager.giveRewards(this, damageMap);
      }
      DataBaseFactory.INSTANCE.saveOrUpdateHistoryRaid(this);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void openStartBattleMenu(ServerPlayerEntity player) {
    if (health <= 0 || finish) {
      PlayerUtils.sendMessage(
        player,
        "§c[§6CobbleRaids§c] §cThis raid has already finished.§r",
        CobbleRaids.language.getPrefix(),
        TypeMessage.CHAT
      );
      return;
    }
    if (DataBaseFactory.INSTANCE.findUserByPlayer(player).isBanned(categoryRaid)) {
      PlayerUtils.sendMessage(
        player,
        "§c[§6CobbleRaids§c] §cYou are banned from this raid category.§r",
        CobbleRaids.language.getPrefix(),
        TypeMessage.CHAT
      );
      return;
    }
    if (!categoryRaid.havePermission(player)) {
      PlayerUtils.sendMessage(
        player,
        CobbleRaids.language.getNotPermission(),
        CobbleRaids.language.getPrefix(),
        TypeMessage.CHAT
      );
      return;
    }
    if (PlayerUtils.isBattle(player)) return;
    CobbleRaids.language.getStartBattleRaid().open(
      player,
      raidData.getActualPhasePokemonItem(this),
      confirm -> {
        startBattle(player);
        UIManager.closeUI(player);
      },
      cancel -> {
        PlayerUtils.sendMessage(
          player,
          "§c[§6CobbleRaids§c] §cYou have cancelled the raid battle.§r",
          CobbleRaids.language.getPrefix(),
          TypeMessage.CHAT
        );
        UIManager.closeUI(player);
      }
    );
  }

  public List<ServerPlayerEntity> getNearPlayers() {
    return categoryRaid.getWorld().getPlayers(player ->
      player.getPos().isInRange(
        categoryRaid.getCoords().getVec3d(),
        64
      )
    );
  }

  public boolean isFinishByTime() {
    return System.currentTimeMillis() >= endTime;
  }

  private boolean preNotifyActionBar = false;

  public void sendActionBarTimeLeft() {
    long actionBar = CobbleRaids.config.getStartSendActionBar().toMillis(); // 15000 ms = 15 seconds
    if (System.currentTimeMillis() < startTime - actionBar && System.currentTimeMillis() < endTime - actionBar) {
      if (CobbleRaids.config.isDebug()) {
        CobbleUtils.LOGGER.info("[RaidActionBar] Not sending action bar for raid " + raidUUID);
      }
      return;
    }
    if (startTime >= System.currentTimeMillis()) {
      if (!preNotifyActionBar) {
        preNotifyActionBar = true;
        RaidEvents.RAID_STARTED_PRE.emit(new RaidPreStarted(this));
      }
      // Raid waiting to start
      if (CobbleRaids.config.isDebug()) {
        CobbleUtils.LOGGER.info("[RaidActionBar-PreStart] Sending action bar for raid " + raidUUID);
      }
      String format = PlayerUtils.getCooldown(startTime);
      var players = CobbleRaids.server.getPlayerManager().getPlayerList();
      Text text = AdventureTranslator.toNative(
        CobbleRaids.language.getActionBarRaidTimeStart()
          .replace("%time%", format),
        CobbleRaids.language.getPrefix()
      );
      for (var player : players) {
        player.sendMessage(text, true);
      }
    } else if (endTime >= System.currentTimeMillis()) {
      if (raidEntity == null) {
        CobbleRaids.server.execute(() -> raidEntity = generateRaidEntity(false));
      }
      // Raid active in progress
      if (CobbleRaids.config.isDebug()) {
        CobbleUtils.LOGGER.info("[RaidActionBar-Active] Sending action bar for raid " + raidUUID);
      }
      String format = PlayerUtils.getCooldown(endTime);
      var players = getNearPlayers();
      Text text = AdventureTranslator.toNative(
        CobbleRaids.language.getActionBarRaidTimeLeft()
          .replace("%time%", format),
        CobbleRaids.language.getPrefix()
      );
      for (var player : players) {
        player.sendMessage(text, true);
      }
    }
  }

  private void glowing() {
    if (raidEntity == null) return;
    CobbleRaids.server.execute(() -> {
      raidEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, Integer.MAX_VALUE, Integer.MAX_VALUE));

      ServerScoreboard scoreboard = CobbleRaids.server.getScoreboard();

      Team team = scoreboard.getTeam("raid_" + categoryRaid.getId());
      if (team == null) {
        team = scoreboard.addTeam("raid_" + categoryRaid.getId());
      }
      team.setColor(categoryRaid.getGlowingColor());

      scoreboard.addScoreHolderToTeam(raidEntity.getUuid().toString(), team);
    });
  }

  public List<ServerPlayerEntity> getPlayersInWorld() {
    return categoryRaid.getWorld().getPlayers();
  }
}
