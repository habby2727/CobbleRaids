package com.kingpixel.cobbleraids.mixins;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.config.RaidsConfig;
import com.kingpixel.cobbleraids.managers.BattleManager;
import com.kingpixel.cobbleraids.managers.PreStartRaid;
import com.kingpixel.cobbleraids.model.PokemonRaid;
import com.kingpixel.cobbleraids.model.Raid;
import com.kingpixel.cobbleraids.model.RaidStarted;
import com.kingpixel.cobbleraids.model.TypeRaid;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Author: Carlos Varas Alonso - 19/01/2025 1:35
 */
@Mixin(MinecraftServer.class)
public class ShowMessageMixin {
  @Unique private int cobbleRaids$tickCounter = 0;
  @Unique private static Raid raid;
  @Unique private static PokemonRaid pokemonRaid;
  @Unique private Pokemon pokemon;


  @Unique public void cobbleRaids$setRaid(Raid raid) {
    ShowMessageMixin.raid = raid;
    ShowMessageMixin.pokemonRaid = raid.getPokemonRaid();
  }

  @Inject(method = "tick", at = @At("HEAD"))
  private void tick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
    cobbleRaids$tickCounter++;
    if (cobbleRaids$tickCounter % 20 != 0) return;

    if (!BattleManager.activeRaids.isEmpty()) {
      Date now = new Date();
      for (RaidStarted activeRaid : BattleManager.activeRaids) {
        if (activeRaid == null) continue;
        if (activeRaid.isFinished()) continue;
        activeRaid.sendBossBar();
        cobbleRaids$sendMessage(activeRaid);
        if (activeRaid.getFinishTime().before(now)) {
          activeRaid.finish();
        }
      }
    }

    if (!CobbleRaids.config.getDays().contains(LocalDate.now().getDayOfWeek())) {
      CobbleRaids.startDate = null;
      raid = null;
      pokemonRaid = null;
      pokemon = null;
      return;
    }


    if (CobbleRaids.startDate != null) {
      Date start = CobbleRaids.startDate;
      Date now = new Date();
      long diff = start.getTime() - now.getTime();
      // Si faltan 3 minutos par que empiece obtener el pokemonraid
      boolean show = diff <= (long) CobbleRaids.config.getStartShowBar() * 60 * 1000;
      if (!show) {
        raid = null;
        pokemonRaid = null;
        pokemon = null;
      }
      if (show && (raid == null || pokemonRaid == null)) {
        Raid assign;
        if (PreStartRaid.raid != null) {
          assign = PreStartRaid.raid;
          PreStartRaid.raid = null;
        } else {
          assign = RaidsConfig.getRandomRaid(TypeRaid.GLOBAL);
        }
        raid = assign;
        pokemonRaid = raid.getPokemonRaid();
      }

      if (pokemonRaid != null && show) {
        if (pokemon == null) {
          pokemon = PokemonProperties.Companion.parse(pokemonRaid.getPokemon()).create();
        }
        var players = CobbleRaids.server.getPlayerManager().getPlayerList();
        if (players == null) return;
        if (players.isEmpty()) return;
        for (ServerPlayerEntity player : players) {
          if (player == null) continue;
          PlayerUtils.sendMessage(
            player,
            CobbleRaids.language.getMessageStartingRaid()
              .replace("%raid%", raid.getName())
              .replace("%pokemon%", pokemon.showdownId())
              .replace("%cooldown%", PlayerUtils.getCooldown(start))
              .replace("%warp%", raid.getWarp()),
            CobbleRaids.config.getPrefix(),
            TypeMessage.ACTIONBAR
          );
        }
      }

      if (start.before(now)) {
        try {
          BattleManager.startRaid(raid, pokemonRaid);
          CobbleRaids.startDate = null;
          raid = null;
          pokemonRaid = null;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    cobbleRaids$tickCounter = 0;
  }

  @Unique
  private void cobbleRaids$sendMessage(RaidStarted raidStarted) {
    String message = CobbleRaids.language.getMessageRaidFinishing()
      .replace("%raid%", raidStarted.getRaid().getName())
      .replace("%cooldown%", PlayerUtils.getCooldown(raidStarted.getFinishTime()));

    List<ServerPlayerEntity> players = switch (raidStarted.getRaid().getType()) {
      case GLOBAL -> CobbleRaids.server.getPlayerManager().getPlayerList();
      case PLAYER -> raidStarted.getRaidEntity().getEntityWorld().getEntitiesByClass(
        ServerPlayerEntity.class,
        Box.from(raidStarted.getRaidEntity().getPos()).expand(64),
        player -> true
      );
    };
    if (players == null || players.isEmpty()) return;
    for (ServerPlayerEntity player : players) {
      PlayerUtils.sendMessage(
        player,
        message,
        CobbleRaids.config.getPrefix(),
        TypeMessage.ACTIONBAR
      );
    }
  }
}

