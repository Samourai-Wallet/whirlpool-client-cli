package com.samourai.whirlpool.cli.utils;

import com.samourai.whirlpool.client.exception.NotifiableException;
import java.io.Console;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Scanner;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliUtils {
  private static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String LOG_SEPARATOR = "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿";

  public static String generateUniqueString() {
    return UUID.randomUUID().toString().replace("-", "");
  }

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

  public static String readUserInputRequired(String message, boolean secret) {
    message = "⣿ INPUT REQUIRED ⣿ " + message;
    return readUserInput(message, secret, true);
  }

  public static String readUserInput(String message, boolean secret, boolean scannerFallback) {
    Console console = System.console();
    String inviteMessage = message + ">";

    // read line
    String line = null;
    if (console != null) {
      console.printf(inviteMessage);
      line = secret ? new String(console.readPassword()) : console.readLine();
    } else if (scannerFallback) {
      // allow console redirection
      Scanner input = new Scanner(System.in);
      System.out.print(inviteMessage);
      line = input.nextLine();
    }
    if (line != null) {
      line = line.trim();
      if (line.isEmpty()) {
        return null;
      }
    }
    return line;
  }

  public static Character readChar() {
    Console console = System.console();
    if (console != null) {
      try {
        return (char) console.reader().read();
      } catch (IOException e) {
        return null;
      }
    }
    return null;
  }

  public static void notifyError(String message) {
    log.error("⣿ ERROR ⣿ " + message);
  }
}
