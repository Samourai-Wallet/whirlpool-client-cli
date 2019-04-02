package com.samourai.whirlpool.cli.utils;

import com.samourai.api.client.beans.UnspentResponse.UnspentOutput;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoConfig;
import java.io.Console;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Iterator;
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

  public static void logWhirlpoolUtxos(Collection<WhirlpoolUtxo> utxos) {
    String lineFormat = "| %10s | %10s | %70s | %50s | %16s | %10s | %10s | %6s |\n";
    StringBuilder sb = new StringBuilder();
    sb.append(
        String.format(
            lineFormat,
            "BALANCE",
            "CONFIRMS",
            "UTXO",
            "ADDRESS",
            "PATH",
            "STATUS",
            "POOL",
            "MIXS"));
    sb.append(String.format(lineFormat, "(btc)", "", "", "", "", "", "", ""));
    Iterator var3 = utxos.iterator();

    while (var3.hasNext()) {
      WhirlpoolUtxo whirlpoolUtxo = (WhirlpoolUtxo) var3.next();
      WhirlpoolUtxoConfig utxoConfig = whirlpoolUtxo.getUtxoConfig();
      UnspentOutput o = whirlpoolUtxo.getUtxo();
      String utxo = o.tx_hash + ":" + o.tx_output_n;
      sb.append(
          String.format(
              lineFormat,
              ClientUtils.satToBtc(o.value),
              o.confirmations,
              utxo,
              o.addr,
              o.getPath(),
              whirlpoolUtxo.getStatus().name(),
              utxoConfig.getPoolId() != null ? utxoConfig.getPoolId() : "-",
              utxoConfig.getMixsDone() + "/" + utxoConfig.getMixsTarget()));
    }

    log.info("\n" + sb.toString());
  }
}
