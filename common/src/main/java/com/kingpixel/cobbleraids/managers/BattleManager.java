package com.kingpixel.cobbleraids.managers;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.model.PokemonRaid;
import com.kingpixel.cobbleraids.model.Raid;
import com.kingpixel.cobbleraids.model.RaidStarted;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Carlos Varas Alonso - 17/01/2025 22:47
 */
@Getter
@Setter
@ToString
public class BattleManager {
  public static List<RaidStarted> activeRaids = new CopyOnWriteArrayList<>();

  public static void startRaid(Raid raid, PokemonRaid pokemonRaid) {
    RaidStarted raidStarted = new RaidStarted(raid, pokemonRaid);
    if (raidStarted.getRaid() == null) {
      CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "Raid not found");
      return;
    }
    List<ServerPlayerEntity> players = new ArrayList<>();
    switch (raid.getType()) {
      case GLOBAL -> players = CobbleRaids.server.getPlayerManager().getPlayerList();
      case PLAYER -> players = raidStarted.getRaidEntity().getEntityWorld().getEntitiesByClass(ServerPlayerEntity.class,
        new Box(raidStarted.getRaidEntity().getBlockPos()).expand(64), player -> true);
    }
    StatusEffectInstance status = new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0, false, false, false);
    if (!players.isEmpty()) {
      for (ServerPlayerEntity player : players) {
        if (player == null) continue;
        player.addStatusEffect(status);
        raid.getSound().start(player);
        PlayerUtils.sendMessage(
          player,
          CobbleRaids.language.getMessageStartRaid()
            .replace("%raid%", raid.getName())
            .replace("%player%", player.getGameProfile().getName()),
          CobbleRaids.config.getPrefix(),
          TypeMessage.CHAT
        );
      }
    }
    activeRaids.add(raidStarted);
  }

  public static RaidStarted getActiveRaid(UUID uuid) {
    return activeRaids.stream().filter(raidStarted -> raidStarted.getRaidUUID().equals(uuid)).findFirst().orElse(null);
  }
}
