package com.kingpixel.cobbleraids.config;

import com.google.gson.Gson;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.messages.HiperMessage;
import com.kingpixel.cobbleutils.Model.messages.HiperMessageBuilder;
import com.kingpixel.cobbleutils.Model.messages.MessageType;
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

  //LeaderBoard
  private String leaderBoardTitle;
  private String leaderBoardLine;
  private String leaderBoardFooter;

  private HiperMessage messageOutCaptureSession;
  private HiperMessage messageRaidBall;
  // Messages with HiperFormat
  private HiperMessage messagePreStartRaid;
  private HiperMessage messageStartRaid;
  private HiperMessage messageNewPhaseRaid;
  private HiperMessage messageEndRaidKilled;
  private HiperMessage messageEndRaidNotKilled;
  private HiperMessage messageNotLuckyCapture;
  private HiperMessage messageStartCapture;
  private HiperMessage messageEndCapture;

  private ConfirmMenu startBattleRaid;

  /**
   * Constructor to generate a file if one doesn't exist.
   */
  public Lang() {
    prefix = "&6[&eCobbleRaids&6] &r";
    reload = "%prefix% &aConfig reloaded.";
    messageReceivedTicket = "%prefix% &aYou have received %amount% tickets. You now have %total% tickets. Category: " +
      "%category%";
    actionBarRaidTimeLeft = "%prefix% &eTime left: &6%time%";
    actionBarRaidTimeStart = "%prefix% &eStarting in: &6%time%";
    actionBarCaptureTimeLeft = "%prefix% &eCapture time left: &6%time%";
    actionBarCaptureStartingIn = "%prefix% &eStarting in: &6%time%";
    notPermission = "%prefix% &cYou do not have permission to do that.";
    messageStartBattle = "%prefix% &aYou have started a battle raid against &e%pokemon%&a. Good luck!";

    leaderBoardTitle = "&6&lRaid Leaderboard\n";
    leaderBoardLine = "&e%position%. &6%player% &e- &6%damage% damage\n";
    leaderBoardFooter = "&eThanks for playing!";

    messageOutCaptureSession = HiperMessageBuilder.builder()
      .setType(MessageType.TITLE_SUBTITLE)
      .setTitle("&c&lYou are out of the capture session!")
      .setSubtitle("&eYou cant use Raid Balls outside the capture session.")
      .build();
    messageRaidBall = HiperMessageBuilder.builder()
      .setType(MessageType.TITLE_SUBTITLE)
      .setTitle("&6&lRaid Ball")
      .setSubtitle("&cYou need to use a Raid Ball to capture raid bosses!")
      .build();

    messagePreStartRaid = new HiperMessage("titlesubtitle_broadcast: title:&6&lRaid Incoming! subtitle: &ePrepare for" +
      " battle against &6%pokemon%&e! sound:minecraft:entity.dragon_fireball.explode volume:1.0 pitch:1.0",
      null);
    messageStartRaid = new HiperMessage("titlesubtitle_broadcast: title:&c&lRaid Started! subtitle: &eDefeat " + "&6%pokemon%&e! " +
      "sound:minecraft:entity.ender_dragon.growl volume:1.0 pitch:1.0",
      null);
    messageNotLuckyCapture = new HiperMessage("titlesubtitle: title:&c&lOh no! subtitle:&eYou were not lucky enough to " +
      "capture &6%pokemon%&e. Better luck next time! sound:minecraft:entity.villager.no volume:1.0 pitch:1.0",
      null);
    messageNewPhaseRaid = new HiperMessage("titlesubtitle_broadcast: title:&e&lNew Phase! subtitle:&eThe raid boss has " +
      "changed to &6%pokemon%&e! sound:minecraft:block.note_block.pling volume:1.0 pitch:1.0",
      null);
    messageEndRaidKilled = new HiperMessage("titlesubtitle_broadcast: title:&6&lRaid Finished! subtitle:&eThe raid has ended. " +
      "Thanks for playing! sound:minecraft:entity.player.levelup volume:1.0 pitch:1.0",
      null);
    messageEndRaidNotKilled = new HiperMessage("titlesubtitle_broadcast: title:&c&lRaid Ended! subtitle:&eThe raid has ended " +
      "without defeating the boss. sound:minecraft:entity.villager.no volume:1.0 pitch:1.0",
      null);
    messageStartCapture = new HiperMessage("titlesubtitle: title:&a&lCapture Started! subtitle:&eYou can now try to " +
      "capture &6%pokemon%&e! sound:minecraft:block.note_block.chime volume:1.0 pitch:1.0",
      null);
    messageEndCapture = new HiperMessage("titlesubtitle: title:&c&lCapture Ended! subtitle:&eThe capture session has ended. " +
      "sound:minecraft:block.note_block.bass volume:1.0 pitch:1.0", null);
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
