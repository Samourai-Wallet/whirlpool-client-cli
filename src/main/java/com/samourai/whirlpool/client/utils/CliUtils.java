package com.samourai.whirlpool.client.utils;

import com.samourai.api.client.beans.UnspentResponse;
import com.samourai.rpc.client.RpcClientService;
import com.samourai.whirlpool.client.exception.BroadcastException;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import java.io.Console;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliUtils {
  private static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static List<UnspentResponse.UnspentOutput> filterUtxoMustMix(
      Pool pool, List<UnspentResponse.UnspentOutput> utxos) {
    long balanceMin =
        WhirlpoolProtocol.computeInputBalanceMin(
            pool.getDenomination(), false, pool.getMinerFeeMin());
    long balanceMax =
        WhirlpoolProtocol.computeInputBalanceMax(
            pool.getDenomination(), false, pool.getMinerFeeMax());
    List<UnspentResponse.UnspentOutput> mustMixUtxos =
        utxos
            .stream()
            .filter(utxo -> utxo.value >= balanceMin && utxo.value <= balanceMax)
            .collect(Collectors.toList());
    return mustMixUtxos;
  }

  public static List<UnspentResponse.UnspentOutput> filterUtxoUniqueHash(
      List<UnspentResponse.UnspentOutput> utxos) {
    List<UnspentResponse.UnspentOutput> mustMixUtxos =
        utxos
            .stream()
            .filter(distinctByKey(UnspentResponse.UnspentOutput::getTxHash))
            .collect(Collectors.toList());
    return mustMixUtxos;
  }

  public static void broadcastOrNotify(Optional<RpcClientService> rpcClientService, Transaction tx)
      throws Exception {
    if (rpcClientService.isPresent()) {
      rpcClientService.get().broadcastTransaction(tx);
    } else {
      throw new BroadcastException(tx);
    }
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

  public static void broadcastTxInstruction(BroadcastException e) throws NotifiableException {
    String hexTx = new String(Hex.encode(e.getTx().bitcoinSerialize()));
    String message =
        "Please broadcast manually the following transaction (or restart with --rpc-client-url=http://user:password@yourBtcNode:port):\n"
            + hexTx
            + "\n";
    CliUtils.waitUserAction(message);
  }

  public static String sha256Hash(String str) {
    return Sha256Hash.wrap(Sha256Hash.hash(str.getBytes())).toString();
  }
}
