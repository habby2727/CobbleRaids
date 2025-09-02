package com.kingpixel.cobbleraids.model;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 18/01/2025 7:26
 */
public class BlackListRaid {
  private final List<String> pokemons;
  private final List<String> abilities;
  private final List<String> moves;
  private final List<String> items;

  public BlackListRaid() {
    this.pokemons = List.of("pikachu", "charmander", "bulbasaur");
    this.abilities = List.of("blaze", "overgrow", "torrent");
    this.moves = List.of("ember", "tackle", "growl");
    this.items = List.of("cobblemon:master_ball");
  }

  public boolean isBanned(ServerPlayerEntity player, Pokemon pokemon) {
    if (pokemons.contains(pokemon.showdownId())) {
      PlayerUtils.sendMessage(
        player,
        replace(CobbleRaids.language.getMessageBannedAbility(), pokemon),
        CobbleRaids.config.getPrefix(),
        TypeMessage.CHAT
      );
      return true;
    }
    if (abilities.contains(pokemon.getAbility().getName())) {
      PlayerUtils.sendMessage(
        player,
        replace(CobbleRaids.language.getMessageBannedAbility(), pokemon),
        CobbleRaids.config.getPrefix(),
        TypeMessage.CHAT
      );
      return true;
    }
    for (Move move : pokemon.getMoveSet()) {
      if (moves.contains(move.getName())) {
        PlayerUtils.sendMessage(
          player,
          replace(CobbleRaids.language.getMessageBannedMove(), pokemon)
            .replace("%move%", move.getName()),
          CobbleRaids.config.getPrefix(),
          TypeMessage.CHAT
        );
        return true;
      }
    }
    String heldItem = itemKey(pokemon.getHeldItem$common().getTranslationKey());
    if (items.contains(heldItem)) {
      PlayerUtils.sendMessage(
        player,
        replace(CobbleRaids.language.getMessageBannedItem(), pokemon)
          .replace("%item%", heldItem),
        CobbleRaids.config.getPrefix(),
        TypeMessage.CHAT
      );
      return true;
    }

    return false;
  }

  private String itemKey(String item) {
    return item
      .replace("block.", "")
      .replace("item.", "")
      .replace(".", ":");
  }

  private String replace(String message, Pokemon pokemon) {
    return message
      .replace("%pokemon%", pokemon.showdownId())
      .replace("%ability%", pokemon.getAbility().getName());
  }
}
