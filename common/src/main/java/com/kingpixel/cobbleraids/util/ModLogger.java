package com.kingpixel.cobbleraids.util;

import com.kingpixel.cobbleutils.util.UtilsLogger;
import org.apache.logging.log4j.Logger;

public final class ModLogger {
  private final Logger logger;

  public ModLogger(String modId) {
    this.logger = UtilsLogger.getLogger(modId);
  }

  public void info(String message) {
    logger.info(message);
  }

  public void info(String modId, String message) {
    logger.info(message);
  }

  public void warn(String message) {
    logger.warn(message);
  }

  public void warn(String modId, String message) {
    logger.warn(message);
  }

  public void error(String message) {
    logger.error(message);
  }

  public void error(String modId, String message) {
    logger.error(message);
  }

  public void error(String message, Throwable throwable) {
    logger.error(message, throwable);
  }

  public void fatal(String message) {
    logger.fatal(message);
  }

  public void fatal(String modId, String message) {
    logger.fatal(message);
  }

  public void fatal(String message, Throwable throwable) {
    logger.fatal(message, throwable);
  }
}
