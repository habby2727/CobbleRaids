package com.kingpixel.cobbleraids.config;

import com.google.gson.Gson;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.TitleSubTitle;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.ui.ConfirmMenu;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.CompletableFuture;

@Getter
@Setter
public class Lang {
  private String prefix;
  private String reload;
  private String messageReceivedTicket;
  private String actionBarRaidTimeLeft;
  private String actionBarRaidTimeStart;
  private String actionBarCaptureStartingIn;
  private String actionBarCaptureTimeLeft;
  private String notPermission;
  private String messageStartBattle;

  private TitleSubTitle titlePreStartRaid;
  private TitleSubTitle titleStartRaid;
  private TitleSubTitle titleNewPhaseRaid;
  private TitleSubTitle titleEndRaid;
  private TitleSubTitle titleNotLuckyCapture;
  private TitleSubTitle titleStartCapture;
  private TitleSubTitle titleEndCapture;

  private ConfirmMenu startBattleRaid;

  /**
   * Constructor to generate a file if one doesn't exist.
   */
  public Lang() {
    prefix = "&6[&eCobbleRaids&6] &r";
    reload = "%prefix% &aConfig reloaded.";
    messageReceivedTicket = "%prefix% &aYou have received %amount% tickets. You now have %total% tickets. Category: " +
      "%category%";
    titleNotLuckyCapture = new TitleSubTitle("&c&lOh no!", "&eYou were not lucky enough to capture &6%pokemon%&e. Better " +
      "luck next time!");
    actionBarRaidTimeLeft = "&eTime left: &6%time%";
    actionBarRaidTimeStart = "&eStarting in: &6%time%";
    actionBarCaptureTimeLeft = "&eCapture time left: &6%time%";
    actionBarCaptureStartingIn = "&eStarting in: &6%time%";
    notPermission = "%prefix% &cYou do not have permission to do that.";
    messageStartBattle = "%prefix% &aYou have started a battle raid against &e%pokemon%&a. Good luck!";
    titlePreStartRaid = new TitleSubTitle("&6&lRaid Incoming!", "&ePrepare for battle against &6%pokemon%&e!");
    titleStartRaid = new TitleSubTitle("&c&lRaid Started!", "&eDefeat &6%pokemon%&e!");
    titleNewPhaseRaid = new TitleSubTitle("&e&lNew Phase!", "&eThe raid boss has changed to &6%pokemon%&e!");
    titleEndRaid = new TitleSubTitle("&6&lRaid Finished!", "&eThe raid has ended. Thanks for playing!");
    titleStartCapture = new TitleSubTitle("&a&lCapture Started!", "&eYou can now try to capture &6%pokemon%&e!");
    titleEndCapture = new TitleSubTitle("&c&lCapture Ended!", "&eThe capture session has ended.");
    startBattleRaid = new ConfirmMenu();
    startBattleRaid.setTitle("Start Battle Raid?");
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
