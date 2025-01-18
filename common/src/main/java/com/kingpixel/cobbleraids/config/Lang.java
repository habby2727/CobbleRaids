package com.kingpixel.cobbleraids.config;

import com.google.gson.Gson;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.ui.MenuRewards;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.CompletableFuture;

@Getter
@Setter
public class Lang {
  private String messageReload;
  private String messageBannedPokemon;
  private String messageBannedAbility;
  private String messageBannedItem;
  private String messageBannedMove;
  private String messageBannedNature;
  private String messageBannedType;
  private MenuRewards menuRewards;

  /**
   * Constructor to generate a file if one doesn't exist.
   */
  public Lang() {
    this.messageReload = "&aReloaded!";
    this.messageBannedPokemon = "&cYour Pokémon %pokemon% is banned!";
    this.messageBannedAbility = "&cYour Pokémon %pokemon% has a banned ability: %ability%!";
    this.messageBannedItem = "&cYour Pokémon %pokemon% is holding a banned item: %item%!";
    this.messageBannedMove = "&cYour Pokémon %pokemon% has this move: %move% in the moveSet!";
    this.messageBannedNature = "&cYour Pokémon %pokemon% has a banned nature: %nature%!";
    this.messageBannedType = "&cYour Pokémon %pokemon% is of a banned type: %type%!";
    this.menuRewards = new MenuRewards();
  }

  /**
   * Method to initialize the config.
   */
  public void init() {
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(CobbleRaids.PATH_LANG, CobbleRaids.config.getLang() + ".json",
      el -> {
        Gson gson = Utils.newGson();
        CobbleRaids.language = gson.fromJson(el, Lang.class);
        String data = gson.toJson(CobbleRaids.language);
        CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleRaids.PATH_LANG, CobbleRaids.config.getLang() +
            ".json",
          data);
        if (!futureWrite.join()) {
          CobbleUtils.LOGGER.fatal(CobbleRaids.MOD_ID, "Could not write lang.json file for " + CobbleRaids.MOD_NAME + ".");
        }
      });

    if (!futureRead.join()) {
      CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "No lang.json file found for" + CobbleRaids.MOD_NAME + ". Attempting " +
        "to generate one.");
      Gson gson = Utils.newGson();
      String data = gson.toJson(this);
      CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleRaids.PATH_LANG, CobbleRaids.config.getLang() +
          ".json",
        data);

      if (!futureWrite.join()) {
        CobbleUtils.LOGGER.fatal(CobbleRaids.MOD_ID, "Could not write lang.json file for " + CobbleRaids.MOD_NAME + ".");
      }
    }
  }


}
