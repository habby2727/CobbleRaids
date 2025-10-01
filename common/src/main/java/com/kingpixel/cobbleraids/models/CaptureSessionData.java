package com.kingpixel.cobbleraids.models;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.Model.messages.HiperMessage;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import kotlin.Unit;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Carlos Varas Alonso - 13/09/2025 21:37
 */
@Data
public class CaptureSessionData {
  public static final String CAPTURE_NBT_KEY = "capture_session";
  public static final Set<String> REMOVE_PERSISTENT_DATA = Set.of(
    CAPTURE_NBT_KEY,
    Raid.RAID_CATEGORY_NBT_KEY,
    Raid.RAID_NBT_KEY
  );
  private boolean started;
  private UUID battleUUID;
  private RaidData raidData;
  private PokemonEntity pokemon;
  private ServerPlayerEntity player;
  private long startTime;
  private long endTime;

  public CaptureSessionData(Raid raid, ServerPlayerEntity player) {
    this.started = false;
    this.battleUUID = UUID.randomUUID(); // Fake UUID, will be replaced when battle starts
    this.pokemon = null;
    this.player = player;
    this.raidData = raid.getRaidData();
    this.startTime = System.currentTimeMillis() + raid.getCategoryRaid().getTimeBeforeStart().toMillis();
    this.endTime = raid.getCategoryRaid().getDurationCapture().toMillis() + startTime;
  }


  public void startSession() {
    HiperMessage message = CobbleRaids.language.getMessageStartCapture();
    message.sendMessage(player, PokemonUtils.replace(message.getRawMessage(), raidData.getCapturePokemonInstance()), CobbleRaids.language.getPrefix(), false);
    CobbleRaids.captureSessionManager.getActiveSessions().remove(battleUUID);
    AtomicReference<UUID> battleUUID = new AtomicReference<>();
    final PokemonEntity[] pokemonEntity = {null};
    Pokemon pokemon = PokemonProperties.Companion.parse(raidData.getCapturePokemon()).create();
    CobbleRaids.server.submit(() -> {
      pokemonEntity[0] = pokemon.sendOut(
        (ServerWorld) player.getWorld(),
        player.getPos(),
        null,
        entity -> {
          return Unit.INSTANCE;
        }
      );
      if (pokemonEntity[0] == null) {
        var msg = AdventureTranslator.toNative(
          "&c[&4!&c] &cError spawning the raid Pokémon."
          , CobbleRaids.language.getPrefix()
        );
        player.sendMessage(msg, false);
        return;
      }

      var party = Cobblemon.INSTANCE.getStorage().getParty(player);
      Pokemon leader = null;
      for (Pokemon p : party) {
        if (p != null) {
          leader = p;
        }
      }
      if (leader == null) return;

      Pokemon finalLeader = leader;

      var startBattle = BattleBuilder.INSTANCE.pve(
        player,
        pokemonEntity[0],
        finalLeader.getUuid(),
        BattleFormat.Companion.getGEN_9_SINGLES(),
        true,
        true,
        Cobblemon.INSTANCE.getConfig().getDefaultFleeDistance(),
        party
      );

      startBattle.ifSuccessful(pokemonBattle -> {
          battleUUID.set(pokemonBattle.getBattleId());
          return Unit.INSTANCE;
        })
        .ifErrored(erroredBattleStart -> {
          var msg = AdventureTranslator.toNative(
            "&c[&4!&c] &cError starting the session."
            , CobbleRaids.language.getPrefix()
          );
          player.sendMessage(msg, false);
          pokemonEntity[0].remove(net.minecraft.entity.Entity.RemovalReason.DISCARDED);
          return Unit.INSTANCE;
        });
      this.battleUUID = battleUUID.get();
      this.pokemon = pokemonEntity[0];
      this.started = true;
      CobbleRaids.captureSessionManager.getActiveSessions().put(this.battleUUID, this);
    }).join();
  }

  public void checkTimeout() {
    long currentTime = System.currentTimeMillis();
    if (startTime > currentTime) {
      var cooldown = PlayerUtils.getCooldown(startTime);
      Text msg = AdventureTranslator.toNative(CobbleRaids.language.getActionBarCaptureStartingIn()
          .replace("%time%", cooldown)
        , CobbleRaids.language.getPrefix()
      );
      player.sendMessage(msg, true);
    } else if (!started) {
      boolean isInBattle = Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player) != null;
      if (isInBattle) {
        startTime += TimeUnit.SECONDS.toMillis(15);
      } else {
        startSession();
        started = true;
      }
    } else if (currentTime >= endTime) {
      finishSession();
    } else {
      var cooldown = PlayerUtils.getCooldown(endTime);
      Text msg = AdventureTranslator.toNative(CobbleRaids.language.getActionBarCaptureTimeLeft()
          .replace("%time%", cooldown)
        , CobbleRaids.language.getPrefix()
      );
      player.sendMessage(msg, true);
    }

  }

  public void finishSession() {
    HiperMessage message = CobbleRaids.language.getMessageEndCapture();
    message.sendMessage(player, PokemonUtils.replace(message.getRawMessage(), raidData.getCapturePokemonInstance()), CobbleRaids.language.getPrefix(), false);
    CobbleRaids.server.execute(() -> {
      var battle = Cobblemon.INSTANCE.getBattleRegistry().getBattle(battleUUID);
      if (battle != null) battle.stop();
      if (pokemon == null) return;
      pokemon.discard();
    });
    CobbleRaids.captureSessionManager.getActiveSessions().remove(battleUUID);
  }

  public void addTime() {
    var time = TimeUnit.SECONDS.toMillis(10);
    if (endTime - System.currentTimeMillis() > time) return;
    this.endTime += time;
  }

  public void setTime() {
    this.endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(15);
  }
}
