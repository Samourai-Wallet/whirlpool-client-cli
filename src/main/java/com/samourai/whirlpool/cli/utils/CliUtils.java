package com.samourai.whirlpool.cli.utils;

import com.samourai.whirlpool.client.exception.NotifiableException;
import java.io.Console;
import java.lang.invoke.MethodHandles;
import org.bitcoinj.core.Sha256Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliUtils {
  private static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void waitUserAction(String message) throws NotifiableException {
    Console console = System.console();
    if (console != null) {
      log.info("⣿ ACTION REQUIRED ⣿ " + message);
      log.info("Press <ENTER> when ready:");
      console.readLine();
    } else {
      throw new NotifiableException("⣿ ACTION REQUIRED ⣿ " + message);
    }
  }

  public static String readUserInput(String message, boolean secret) throws NotifiableException {
    Console console = System.console();
    if (console != null) {
      console.printf("⣿ INPUT REQUIRED ⣿ " + message + "?>");
      String line = secret ? new String(console.readPassword()).trim() : console.readLine().trim();
      if (line.isEmpty()) {
        return null;
      }
      return line;
    } else {
      throw new NotifiableException("⣿ INPUT REQUIRED ⣿ " + message + "?>");
    }
  }

  public static String sha256Hash(String str) {
    return Sha256Hash.wrap(Sha256Hash.hash(str.getBytes())).toString();
  }
}
