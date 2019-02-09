package com.samourai.whirlpool.cli.utils;

import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import java.io.Console;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.bitcoinj.core.Sha256Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliUtils {
  private static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static List<WhirlpoolUtxo> filterUtxoMustMix(
      Pool pool, Collection<WhirlpoolUtxo> whirlpoolUtxos) {
    long balanceMin =
        WhirlpoolProtocol.computeInputBalanceMin(
            pool.getDenomination(), false, pool.getMinerFeeMin());
    long balanceMax =
        WhirlpoolProtocol.computeInputBalanceMax(
            pool.getDenomination(), false, pool.getMinerFeeMax());
    List<WhirlpoolUtxo> mustMixUtxos =
        whirlpoolUtxos
            .stream()
            .filter(
                whirlpoolUtxo ->
                    whirlpoolUtxo.getUtxo().value >= balanceMin
                        && whirlpoolUtxo.getUtxo().value <= balanceMax)
            .collect(Collectors.toList());
    return mustMixUtxos;
  }

  public static List<WhirlpoolUtxo> filterUtxoUniqueHash(Collection<WhirlpoolUtxo> utxos) {
    List<WhirlpoolUtxo> mustMixUtxos =
        utxos
            .stream()
            .filter(distinctByKey(whirlpoolUtxo -> whirlpoolUtxo.getUtxo().tx_hash))
            .collect(Collectors.toList());
    return mustMixUtxos;
  }

  public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
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
