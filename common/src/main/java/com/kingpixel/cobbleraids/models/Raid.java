package com.kingpixel.cobbleraids.models;

import ca.landonjw.gooeylibs2.api.UIManager;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.drop.DropTable;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.database.DataBaseFactory;
import com.kingpixel.cobbleraids.events.raids.RaidEvents;
import com.kingpixel.cobbleraids.events.raids.models.RaidFinished;
import com.kingpixel.cobbleraids.events.raids.models.RaidNewPhase;
import com.kingpixel.cobbleraids.events.raids.models.RaidPostStarted;
import com.kingpixel.cobbleraids.events.raids.models.RaidPreStarted;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import kotlin.Unit;
import lombok.Data;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
  private ConcurrentHashMap<UUID, Integer> damageMap = new ConcurrentHashMap<>();
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
    this.categoryRaid.checker();
    this.health = categoryRaid.getHealth();
    this.maxHealth = categoryRaid.getHealth();
    actualPhase = raidData.getActualPhase(this);
    this.pokemon = PokemonProperties.Companion.parse(actualPhase).create();
  }

  // Start Raid
  public Raid(RaidData raidData) {
    this.finish = false;
    this.startTime = System.currentTimeMillis();
    this.endTime = this.startTime + raidData.getCategoryRaid().getDurationRaid().toMillis();
    this.raidUUID = UUID.randomUUID();
    this.raidData = raidData;
    this.categoryRaid = raidData.getCategoryRaid();
    this.categoryRaid.checker();
    this.health = categoryRaid.getHealth();
    this.maxHealth = categoryRaid.getHealth();
    this.damageMap = new ConcurrentHashMap<>();
    this.raidEntity = generateRaidEntity(false);
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
      bossBar.setPercent(MathHelper.clamp((float) health / maxHealth, 0f, 1f));
      glowing();
    }
  }

  private String actualPhase = "";

  private void refreshRaidEntity() {
    try {
      var phase = raidData.getActualPhase(this);
      if (phase.equals(actualPhase)) {
        if (CobbleRaids.config.isDebug()) {
          CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "Raid Entity phase unchanged: " +
            raidEntity.getPokemon().getDisplayName(false).getString() +
            " Phase: " + actualPhase
          );
        }
        return;
      }
      if (CobbleRaids.config.isDebug()) {
        CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "Raid Entity phase changed: " +
          raidEntity.getPokemon().getDisplayName(false).getString() +
          " From Phase: " + actualPhase + " To Phase: " + phase
        );
      }
      actualPhase = phase;
      Pokemon p = PokemonProperties.Companion.parse(phase).create();
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
    try {
      if (finish || health <= 0) return;
      UserInfo userInfo = DataBaseFactory.INSTANCE.findUserByPlayer(player);
      if (categoryRaid.isNeedTicket() && !damageMap.containsKey(player.getUuid()) && !userInfo.hasTicket(categoryRaid)) {
        PlayerUtils.sendMessage(
          player,
          "§c[§6CobbleRaids§c] §cYou don't have a ticket to join this raid!§r",
          CobbleRaids.language.getPrefix(),
          TypeMessage.CHAT
        );
        return;
      }


      var party = Cobblemon.INSTANCE.getStorage().getParty(player);
      Pokemon leader = null;
      for (Pokemon pokemon : party) {
        if (pokemon != null && !raidData.isBannedPokemon(pokemon)) {
          leader = pokemon;
          break;
        }
      }
      if (FabricLoader.getInstance().isModLoaded("cobblesize")) {
        for (Pokemon p : party) {
          if (p == null) continue;
          if (p.getPersistentData().getString("size").equals("custom")) continue;
          p.setScaleModifier(0.01f);
        }
      }

      if (leader == null) return;

      PokemonEntity fightEntity = generateRaidEntity(true);

      fightEntity.setDrops(new DropTable());
      if (!CobbleRaids.config.isDebug()) {
        fightEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, Integer.MAX_VALUE, 255, false,
          false));
      }

      fightEntity.getPokemon().getPersistentData().putString(RAID_CATEGORY_NBT_KEY, categoryRaid.getId());

      Pokemon finalLeader = leader;

      var fightData = CobbleRaids.raidManager.getFightingPlayer(player.getUuid());
      if (fightData != null) fightData.stop(true);

      var battle = BattleRegistry.getBattleByParticipatingPlayer(player);
      if (battle != null) battle.stop();
      if (!damageMap.containsKey(player.getUuid())) {
        userInfo.removeTicket(categoryRaid);
        incrementHealth(player);
      }
      var startBattle = BattleBuilder.INSTANCE.pve(
        player,
        fightEntity,
        finalLeader.getUuid(),
        BattleFormat.Companion.getGEN_9_SINGLES(),
        false,
        CobbleRaids.config.isHealthParty(),
        Cobblemon.INSTANCE.getConfig().getDefaultFleeDistance() + 16,
        party
      );
      startBattle.ifSuccessful(pokemonBattle -> {
        try {
          UUID battleId = pokemonBattle.getBattleId();
          CobbleRaids.raidManager.addFightingData(
            battleId,
            new FightData(
              battleId,
              this,
              player,
              fightEntity
            )
          );
          PlayerUtils.sendMessage(
            player,
            PokemonUtils.replace(
              CobbleRaids.language.getMessageStartBattle(),
              fightEntity.getPokemon()
            ),
            CobbleRaids.language.getPrefix(),
            TypeMessage.CHAT
          );
          teleportPlayerOut(player);
        } catch (Exception e) {
          e.printStackTrace();
        }
        return Unit.INSTANCE;
      });
      startBattle.ifErrored(erroredBattleStart -> {
        PlayerUtils.sendMessage(
          player,
          "§c[§6CobbleRaids§c] §cCould not start the battle, please try again later.§r",
          CobbleRaids.language.getPrefix(),
          TypeMessage.CHAT
        );
        CobbleRaids.server.execute(fightEntity::discard);
        return Unit.INSTANCE;
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private synchronized void incrementHealth(ServerPlayerEntity player) {
    int size = damageMap.size() + 1;
    if (size > 1) {
      this.maxHealth += categoryRaid.getHealth();
      this.health += categoryRaid.getHealth();
      refreshBossBar();
    }
    damageMap.put(player.getUuid(), 0);
  }

  // Cache of base Pokémon per result (Pokemon, not PokemonEntity)
  private final ConcurrentHashMap<String, Pokemon> cachedPokemons = new ConcurrentHashMap<>();

  /**
   * Generates a raid Pokémon (still returns PokemonEntity)
   * The Pokemon is cached, so cloning can be done in another thread
   */
  private PokemonEntity generateRaidEntity(boolean fight) {
    var result = raidData.getActualPhase(this).trim();

    // Get or create cached Pokemon (the non-entity object)
    Pokemon basePokemon = cachedPokemons.computeIfAbsent(result, key -> {
      var properties = PokemonProperties.Companion.parse(result + " uncatchable=yes");
      Pokemon p = properties.create();
      p.getMoveSet().getMoves().forEach(m -> m.setCurrentPp(999));
      return p;
    });

    // Clone the cached Pokemon for this raid
    Pokemon clonePokemon = clonePokemon(basePokemon, fight);

    // Prepare world and chunk
    var coords = categoryRaid.getCoords();
    ServerWorld serverWorld = categoryRaid.getWorldInstance();
    if (serverWorld == null) {
      CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "World " + categoryRaid.getWorld() + " not found for raid " + raidUUID);
      throw new IllegalStateException("World " + categoryRaid.getWorld() + " not found for raid " + raidUUID);
    }
    int x = coords.getX() >> 4;
    int z = coords.getZ() >> 4;

    if (!serverWorld.isChunkLoaded(x, z)) serverWorld.getChunkManager().getChunk(x, z, ChunkStatus.FULL, false);

    Vec3d pos = coords.getVec3d();
    if (fight) pos = new Vec3d(pos.x + (Math.random() * 8 - 4), pos.y, pos.z + (Math.random() * 8 - 4));

    // Spawn the Pokémon as an entity
    var pokemonEntity = clonePokemon.sendOut(
      serverWorld,
      pos,
      null,
      entity -> {
        categoryRaid.compute(entity, fight);

        if (fight) entity.getPokemon().getPersistentData().putString(RAID_CATEGORY_NBT_KEY, categoryRaid.getId());
        else entity.getPokemon().getPersistentData().putUuid(RAID_NBT_KEY, raidUUID);

        return Unit.INSTANCE;
      }
    );
    if (pokemonEntity != null) {
      pokemonEntity.setYaw(coords.getYaw());
      pokemonEntity.setPitch(coords.getPitch());
      pokemonEntity.setInvulnerable(true);
      pokemonEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, Integer.MAX_VALUE, 255, false,
        CobbleRaids.config.isDebug()));
      pokemonEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, Integer.MAX_VALUE, 255, false,
        CobbleRaids.config.isDebug()));
      pokemonEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, Integer.MAX_VALUE, 255, false,
        CobbleRaids.config.isDebug()));
    }
    if (pokemonEntity == null) throw new IllegalStateException("Could not create PokemonEntity for raid");

    // Post-creation logic if not a fight
    if (!fight) {
      if (CobbleRaids.config.isDebug()) {
        CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "Raid Entity spawned: " +
          pokemonEntity.getPokemon().getDisplayName(false).getString() +
          " at " + pokemonEntity.getPos().toString()
        );
      }
      this.raidEntity = pokemonEntity;
      glowing();
      RaidEvents.RAID_STARTED_POST.emit(new RaidPostStarted(this));
    }

    return pokemonEntity;
  }

  /**
   * Clones a base Pokemon and applies raid-specific data
   */
  private Pokemon clonePokemon(Pokemon base, boolean fight) {
    Pokemon clone = base.clone(true, DynamicRegistryManager.EMPTY);
    clone.getPersistentData().putUuid(RAID_NBT_KEY, raidUUID);
    if (fight) clone.getPersistentData().putString(RAID_CATEGORY_NBT_KEY, categoryRaid.getId());
    return clone;
  }


  public void teleportPlayerOut(ServerPlayerEntity player) {
    double radio = categoryRaid.getRadio();
    Vec3d coords = categoryRaid.getCoords().getVec3d();
    Vec3d posPlayer = player.getPos();
    if (!posPlayer.isInRange(coords, radio)) return;
    Vec3d direction = posPlayer.subtract(coords).normalize();
    Vec3d newPos = coords.add(direction.multiply(radio + 5));
    CobbleRaids.server.execute(() -> player.teleport(
      categoryRaid.getWorldInstance(),
      newPos.x,
      posPlayer.getY(),
      newPos.z,
      player.getYaw(),
      player.getPitch()
    ));
  }

  public void updateHealth(FightData fightData, int damage) {
    ServerPlayerEntity player = fightData.getPlayer();
    damageMap.computeIfPresent(player.getUuid(), (k, damageFromMap) -> damageFromMap + damage);
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
      openStartBattleMenu(player);
      fightData.stop(true);
    } else finishRaid();
  }

  private synchronized void removeDamage(int damage) {
    this.health -= damage;
  }

  public synchronized void finishRaid() {
    try {
      if (finish) return;
      finish = true;
      boolean killed = health <= 0;
      RaidEvents.RAID_FINISHED.emit(new RaidFinished(this, killed));
      if (bossBar != null) bossBar.clearPlayers();
      CobbleRaids.server.execute(() -> {
        Chunk chunk = null;
        if (raidEntity != null) {
          var pos = raidEntity.getChunkPos();
          chunk = raidEntity.getWorld().getChunk(pos.x, pos.z);
          raidEntity.remove(Entity.RemovalReason.DISCARDED);
        }
        CobbleRaids.raidManager.removeRaid(raidUUID);
        List<FightData> fights = CobbleRaids.raidManager.getFightingsByRaidUUID(raidUUID);
        for (FightData fight : fights) {
          UIManager.closeUI(fight.getPlayer());
          fight.stop(true);
        }
        if (killed) CobbleRaids.rewardsManager.giveRewards(this, damageMap);
        if (chunk != null) chunk.setNeedsSaving(true);
      });
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
    CobbleRaids.language.getStartBattleRaid().open(
      player,
      raidData.getActualPhasePokemonItem(this),
      confirm -> {
        try {
          if (PlayerUtils.isBattle(player)) {
            PlayerUtils.sendMessage(
              player,
              "§c[§6CobbleRaids§c] §cYou are already in a battle.§r",
              CobbleRaids.language.getPrefix(),
              TypeMessage.CHAT
            );
            return;
          }
          PlayerUtils.isCooldownMenu(player, "raid_start_battle", DurationValue.parse("1s"));
          startBattle(player);
          UIManager.closeUI(player);
        } catch (Exception e) {
          e.printStackTrace();
        }
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
    return categoryRaid.getWorldInstance().getPlayers(player ->
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
    if (System.currentTimeMillis() < startTime - actionBar && System.currentTimeMillis() < endTime - actionBar) return;
    if (startTime >= System.currentTimeMillis()) {
      if (!preNotifyActionBar) {
        preNotifyActionBar = true;
        RaidEvents.RAID_STARTED_PRE.emit(new RaidPreStarted(this));
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
    return categoryRaid.getWorldInstance().getPlayers();
  }
}
