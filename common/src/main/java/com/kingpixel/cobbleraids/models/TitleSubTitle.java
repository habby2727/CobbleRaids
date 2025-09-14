package com.kingpixel.cobbleraids.models;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import lombok.Data;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

/**
 * @author Carlos Varas Alonso - 14/09/2025 18:21
 */
@Data
public class TitleSubTitle {
  private String title;
  private String subTitle;

  public TitleSubTitle(String title, String subTitle) {
    this.title = title;
    this.subTitle = subTitle;
  }

  public void send(@Nullable ServerPlayerEntity player, Raid raid) {
    TitleS2CPacket titlePacket = new TitleS2CPacket(
      AdventureTranslator.toNative(raid.replace(title)
      ));
    SubtitleS2CPacket subtitlePacket = new SubtitleS2CPacket(
      AdventureTranslator.toNative(raid.replace(subTitle))
    );
    sendPackets(player, titlePacket, subtitlePacket);
  }

  public String replace(String text, Pokemon pokemon) {
    return PokemonUtils.replace(text, pokemon);
  }

  public void send(@Nullable ServerPlayerEntity player, RaidData raidData) {
    TitleS2CPacket titlePacket = new TitleS2CPacket(
      AdventureTranslator.toNative(replace(title, raidData.getCapturePokemonInstance()))
    );
    SubtitleS2CPacket subtitlePacket = new SubtitleS2CPacket(
      AdventureTranslator.toNative(replace(subTitle, raidData.getCapturePokemonInstance()))
    );
    sendPackets(player, titlePacket, subtitlePacket);
  }

  private void sendPackets(@Nullable ServerPlayerEntity player, TitleS2CPacket titlePacket, SubtitleS2CPacket subtitlePacket) {
    if (player != null) {
      player.networkHandler.sendPacket(titlePacket);
      player.networkHandler.sendPacket(subtitlePacket);
    } else {
      CobbleRaids.server.getPlayerManager().getPlayerList().forEach(serverPlayerEntity -> {
        serverPlayerEntity.networkHandler.sendPacket(titlePacket);
        serverPlayerEntity.networkHandler.sendPacket(subtitlePacket);
      });
    }
  }

}
