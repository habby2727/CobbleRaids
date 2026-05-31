package com.kingpixel.cobbleraids.util;

import java.util.concurrent.ThreadLocalRandom;

public final class ModRandom {
  private ModRandom() {
  }

  public static ThreadLocalRandom current() {
    return ThreadLocalRandom.current();
  }
}
