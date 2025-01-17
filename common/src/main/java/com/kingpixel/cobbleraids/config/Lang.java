package com.kingpixel.cobbleraids.config;

import com.google.gson.Gson;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.CompletableFuture;

@Getter
@Setter
public class Lang {


  /**
   * Constructor to generate a file if one doesn't exist.
   */
  public Lang() {

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
