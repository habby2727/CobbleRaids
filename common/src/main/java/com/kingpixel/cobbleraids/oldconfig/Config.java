package com.kingpixel.cobbleraids.oldconfig;

import com.google.gson.Gson;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.model.BlackListRaid;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 29/04/2024 0:14
 */
@Getter
@Data
@ToString
public class Config {
  private boolean debug;
  private String prefix;
  private String lang;
  private boolean blindnessEffect;
  private boolean moreHealthByEachPlayer;
  private boolean blockXp;
  private boolean needPokeBallRaids;
  private List<String> pokeballs;
  private int overLevel;
  private int cooldown;
  private int startShowBar;
  private int secondsToStartCapture;
  private int secondsToFinishCapture;
  private List<DayOfWeek> days;
  private List<String> commands;
  private BlackListRaid banned;

  public Config() {
    debug = false;
    prefix = "§7[§6CobbleRaids§7] ";
    lang = "en";
    overLevel = 0;
    blindnessEffect = true;
    moreHealthByEachPlayer = true;
    blockXp = true;
    needPokeBallRaids = false;
    pokeballs = List.of("cobblemon:ancient_origin_ball");
    cooldown = 30;
    startShowBar = 5;
    secondsToStartCapture = 60;
    secondsToFinishCapture = 120;
    days = Arrays.stream(DayOfWeek.values()).toList();
    commands = List.of("cobbleraids", "raid");
    banned = new BlackListRaid();
  }

  public void init() {
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(CobbleRaids.PATH, "OldConfig.json",
      el -> {
        Gson gson = Utils.newGson();
        //CobbleRaids.config = gson.fromJson(el, Config.class);
        String data = gson.toJson(CobbleRaids.config);
        CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleRaids.PATH, "OldConfig.json",
          data);
        if (!futureWrite.join()) {
          CobbleUtils.LOGGER.fatal(CobbleRaids.MOD_ID, "Could not write OldConfig.json file for " + CobbleRaids.MOD_NAME +
            ".");
        }
      });

    if (!futureRead.join()) {
      CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "No OldConfig.json file found for" + CobbleRaids.MOD_NAME + ". Attempting" +
        " to generate one.");
      Gson gson = Utils.newGson();
      String data = gson.toJson(this);
      CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleRaids.PATH, "OldConfig.json",
        data);

      if (!futureWrite.join()) {
        CobbleUtils.LOGGER.fatal(CobbleRaids.MOD_ID, "Could not write OldConfig.json file for " + CobbleRaids.MOD_NAME + ".");
      }
    }


  }
}