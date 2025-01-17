package com.kingpixel.cobbleraids.fabric;

import com.kingpixel.cobbleraids.CobbleRaids;
import net.fabricmc.api.ModInitializer;

public class CobbleRaidsFabric implements ModInitializer {
  @Override
  public void onInitialize() {
    CobbleRaids.init();
  }
}
