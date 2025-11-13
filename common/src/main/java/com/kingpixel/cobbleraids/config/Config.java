package com.kingpixel.cobbleraids.config;

import com.google.gson.Gson;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.AdvancedBlacklist;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.Model.DataBaseType;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.Model.WebHookData;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 29/04/2024 0:14
 */
@Getter
@Data
@ToString
public class Config {
  private boolean debug;
  private String lang;
  private boolean healthParty;
  private boolean xpBlock;
  private boolean raidBallEnabled;
  private boolean maintainRaidBalls;
  private DurationValue cooldownBetweenRaids;
  private DurationValue startSendActionBar;
  private Set<String> commands;
  private DataBaseConfig database;
  private WebHookData webhook;
  private AdvancedBlacklist blacklist;


  public Config() {
    debug = false;
    lang = "en";
    healthParty = false;
    raidBallEnabled = true;
    maintainRaidBalls = false;
    xpBlock = true;
    cooldownBetweenRaids = DurationValue.parse("30m");
    startSendActionBar = DurationValue.parse("15m");

    commands = Set.of("raids", "cobbleraids");
    if (database == null) {
      database = new DataBaseConfig();
      database.setDatabase("cobbleraids");
      database.setType(DataBaseType.JSON);
    }
    webhook = new WebHookData("", "", "Raids");
    blacklist = new AdvancedBlacklist();
  }

  public void init() {
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(CobbleRaids.PATH, "config.json",
      el -> {
        Gson gson = Utils.newGson();
        CobbleRaids.config = gson.fromJson(el, Config.class);
        String data = gson.toJson(CobbleRaids.config);
        CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleRaids.PATH, "config.json",
          data);
        if (Boolean.FALSE.equals(futureWrite.join())) {
          CobbleUtils.LOGGER.fatal(CobbleRaids.MOD_ID, "Could not write config.json file for " + CobbleRaids.MOD_NAME +
            ".");
        }
      });

    if (Boolean.FALSE.equals(futureRead.join())) {
      CobbleUtils.LOGGER.info(CobbleRaids.MOD_ID, "No config.json file found for" + CobbleRaids.MOD_NAME + ". Attempting" +
        " to generate one.");
      Gson gson = Utils.newGson();
      String data = gson.toJson(this);
      CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleRaids.PATH, "config.json",
        data);

      if (Boolean.FALSE.equals(futureWrite.join())) {
        CobbleUtils.LOGGER.fatal(CobbleRaids.MOD_ID, "Could not write config.json file for " + CobbleRaids.MOD_NAME + ".");
      }
    }


  }
}