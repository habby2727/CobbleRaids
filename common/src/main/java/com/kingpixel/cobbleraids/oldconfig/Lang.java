package com.kingpixel.cobbleraids.oldconfig;

import com.google.gson.Gson;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.ui.MenuDamageRewards;
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
  private String messageNoRewards;
  private String messageRewardLastHit;
  private String messageRewardDamage;
  private String messageRewardGlobal;
  private String messageLastHit;
  private String messageTableDamage;
  private String formatTableDamage;
  private String messageNotTeleport;
  private String messageStartingRaid;
  private String messageStartRaid;
  private String messageRaidFinishing;
  private String messageFinishRaid;
  private String messageCaptureSessionLuck;
  private String messageCaptureSessionNotLuck;
  private String messageStartingCapture;
  private String messageFinishingCapture;
  private String messageBannedPokemon;
  private String messageBannedAbility;
  private String messageBannedItem;
  private String messageBannedMove;
  private String messageBannedNature;
  private String messageBannedType;
  private String messageEnabledPokeBalls;
  private MenuRewards menuRewards;
  private MenuDamageRewards menuDamageRewards;

  /**
   * Constructor to generate a file if one doesn't exist.
   */
  public Lang() {
    this.messageReload = "%prefix% &aReloaded!";
    this.messageNoRewards = "%prefix% &cNo rewards the raid not be defeated!";
    this.messageRewardLastHit = "%prefix% &aYou obtained the last hit reward!";
    this.messageRewardDamage = "%prefix% &aYou obtained the damage reward %pos%!";
    this.messageRewardGlobal = "%prefix% &aYou obtained the global reward!";
    this.messageLastHit = "%prefix% &aLast hit: %player%!";
    this.messageTableDamage = "%prefix% &aDamage table:\n %table%";
    this.formatTableDamage = " &f- &a%player%&7: &c%damage%";
    this.messageNotTeleport = "%prefix% &cNot raid active!";
    this.messageStartingRaid = "%prefix% &aStarting raid in: %cooldown%. Warp: %warp%!";
    this.messageStartRaid = "%prefix% &aRaid started!";
    this.messageRaidFinishing = "%prefix% &aRaid finishing in: %cooldown%!";
    this.messageFinishRaid = "%prefix% &aRaid finished!";
    this.messageBannedPokemon = "%prefix% &cYour Pokémon %pokemon% is banned!";
    this.messageBannedAbility = "%prefix% &cYour Pokémon %pokemon% has a banned ability: %ability%!";
    this.messageBannedItem = "%prefix% &cYour Pokémon %pokemon% is holding a banned item: %item%!";
    this.messageBannedMove = "%prefix% &cYour Pokémon %pokemon% has this banned move: %move% in the moveSet!";
    this.messageBannedNature = "%prefix% &cYour Pokémon %pokemon% has a banned nature: %nature%!";
    this.messageBannedType = "%prefix% &cYour Pokémon %pokemon% is of a banned type: %type%!";
    this.messageCaptureSessionLuck = "%prefix% &aYou have luck, you enter to capture session raid!";
    this.messageCaptureSessionNotLuck = "%prefix% &cYou don't have luck, you can't enter to capture session raid!";
    this.messageStartingCapture = "%prefix% &aStarting capture session in: %cooldown%!";
    this.messageFinishingCapture = "%prefix% &aCapture session finishing in: %cooldown%!";
    this.messageEnabledPokeBalls = "%prefix% &aEnabled pokeballs: %pokeballs%!";
    this.menuRewards = new MenuRewards();
    this.menuDamageRewards = new MenuDamageRewards();
  }

  /**
   * Method to initialize the OldConfig.
   */
  public void init() {
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(CobbleRaids.PATH_LANG, CobbleRaids.config.getLang() + ".json",
      el -> {
        Gson gson = Utils.newGson();
        //CobbleRaids.language = gson.fromJson(el, Lang.class);
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
