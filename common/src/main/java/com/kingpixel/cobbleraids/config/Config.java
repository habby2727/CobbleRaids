package com.kingpixel.cobbleraids.config;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.models.AdvancedBlacklist;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.Model.DataBaseType;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.Model.WebHookData;
import com.kingpixel.cobbleutils.util.UtilsFile;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.util.Set;

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
    var configFile = CobbleUtils.getPath().resolve(CobbleRaids.MOD_ID).resolve("config.json");
    if (!UtilsFile.exists(configFile)) {
      CobbleRaids.LOGGER.info("No config.json file found for " + CobbleRaids.MOD_NAME + ". Attempting to generate one.");
    }
    try {
      CobbleRaids.config = UtilsFile.readOrCreate(configFile, Config.class, Config::new);
      UtilsFile.write(configFile, CobbleRaids.config);
    } catch (IOException e) {
      CobbleRaids.LOGGER.fatal("Could not load config.json file for " + CobbleRaids.MOD_NAME + ".", e);
      throw new IllegalStateException("Could not load config.json file for " + CobbleRaids.MOD_NAME + ".", e);
    }
  }
}
