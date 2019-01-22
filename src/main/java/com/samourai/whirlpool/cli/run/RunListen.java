package com.samourai.whirlpool.cli.run;

import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunListen {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public RunListen() {}

  public void run() {
    log.info("Starting listening...");
    while (true) {
      log.info("Listening...");
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
      }
    }
  }
}
