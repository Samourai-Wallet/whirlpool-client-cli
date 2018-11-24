package com.samourai.whirlpool.client.run;

import com.samourai.api.SamouraiApi;
import com.samourai.api.beans.UnspentResponse;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.client.exception.BroadcastException;
import com.samourai.whirlpool.client.utils.Bip84ApiWallet;
import com.samourai.whirlpool.client.utils.CliUtils;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunLoopWallet {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int OUTPUTS_PER_TX0 = 5;

  private WhirlpoolClientConfig config;
  private RunTx0 runTx0;
  private RunMixWallet runMixWallet;
  private Bip84ApiWallet depositAndPremixWallet;
  private Optional<RunAggregateAndConsolidateWallet> runAggregateAndConsolidateWallet;

  public RunLoopWallet(
      WhirlpoolClientConfig config,
      RunTx0 runTx0,
      RunMixWallet runMixWallet,
      Bip84ApiWallet depositAndPremixWallet,
      Optional<RunAggregateAndConsolidateWallet> runAggregateAndConsolidateWallet) {
    this.config = config;
    this.runTx0 = runTx0;
    this.runMixWallet = runMixWallet;
    this.depositAndPremixWallet = depositAndPremixWallet;
    this.runAggregateAndConsolidateWallet = runAggregateAndConsolidateWallet;
  }

  public boolean run(Pool pool, int nbClients, String feePaymentCode) throws Exception {
    // fetch unspent utx0s
    log.info(" • Fetching unspent outputs from premix...");
    List<UnspentResponse.UnspentOutput> utxos =
        depositAndPremixWallet.fetchUtxos().stream().collect(Collectors.toList());

    if (log.isDebugEnabled()) {
      log.debug("Found " + utxos.size() + " utxo from premix:");
      CliUtils.printUtxos(utxos);
    }

    // find mustMixUtxos
    List<UnspentResponse.UnspentOutput> mustMixUtxosUnique =
        CliUtils.filterUtxoUniqueHash(CliUtils.filterUtxoMustMix(pool, utxos));
    if (log.isDebugEnabled()) {
      log.debug("Found " + mustMixUtxosUnique.size() + " unique mustMixUtxo");
    }

    // find liquidityUtxos
    List<UnspentResponse.UnspentOutput> liquidityUtxos = new ArrayList<>(); // TODO

    int missingMustMixUtxos =
        computeMissingMustMixUtxos(nbClients, mustMixUtxosUnique, liquidityUtxos);

    // do we have enough mustMixUtxo?
    if (missingMustMixUtxos > 0) {
      // not enough mustMixUtxos => Tx0
      for (int i = 0; i < missingMustMixUtxos; i++) {
        log.info(" • Tx0 (" + (i + 1) + "/" + missingMustMixUtxos + ")...");
        doRunTx0(pool, missingMustMixUtxos, feePaymentCode);

        log.info("Refreshing utxos...");
        Thread.sleep(SamouraiApi.SLEEP_REFRESH_UTXOS);
      }

      // recursive
      return run(pool, nbClients, feePaymentCode);
    } else {
      log.info(" • New mix...");
      return runMixWallet.runMix(mustMixUtxosUnique, pool);
    }
  }

  private void doRunTx0(Pool pool, int missingMustMixUtxos, String feePaymentCode)
      throws Exception {
    try {
      runTx0.runTx0(pool, OUTPUTS_PER_TX0, feePaymentCode);
    } catch (BroadcastException e) {
      throw e;
    } catch (Exception e) {
      log.error("Tx0 failed: " + e.getMessage());

      // premixAndDeposit is empty => autoRefill when possible
      long missingBalance =
          missingMustMixUtxos * (OUTPUTS_PER_TX0 * pool.getDenomination() + RunTx0.SAMOURAI_FEES);
      autoRefill(missingBalance);

      doRunTx0(pool, missingMustMixUtxos, feePaymentCode);
    }
  }

  private void autoRefill(long missingBalance) throws Exception {
    String depositAddress =
        Bech32UtilGeneric.getInstance()
            .toBech32(depositAndPremixWallet.getNextAddress(), config.getNetworkParameters());
    String message =
        "depositAndPremixWallet is empty. I need at least "
            + CliUtils.satToBtc(missingBalance)
            + "btc (+ fees) to continue.\nPlease make a deposit to "
            + depositAddress;
    if (!runAggregateAndConsolidateWallet.isPresent()) {
      CliUtils.waitUserAction(message);
      return;
    }

    // auto aggregate postmix
    log.info(" • depositAndPremixWallet wallet is empty. Aggregating postmix to refill it...");
    boolean aggregateSuccess = runAggregateAndConsolidateWallet.get().run();
    if (!aggregateSuccess) {
      CliUtils.waitUserAction(message);
    }

    log.info("Refreshing utxos...");
    Thread.sleep(SamouraiApi.SLEEP_REFRESH_UTXOS);
  }

  private int computeMissingMustMixUtxos(
      int nbClients,
      List<UnspentResponse.UnspentOutput> mustMixUtxos,
      List<UnspentResponse.UnspentOutput> liquidityUtxos) {
    int missingAnonymitySet = nbClients - (mustMixUtxos.size() + liquidityUtxos.size());
    int missingMustMixUtxos = missingAnonymitySet > 0 ? missingAnonymitySet : 0;
    if (log.isDebugEnabled()) {
      log.debug(
          "Next mix needs "
              + nbClients
              + " utxos. I have "
              + mustMixUtxos.size()
              + " unique mustMixUtxo and "
              + liquidityUtxos.size()
              + " unique liquidityUtxo =>  "
              + missingMustMixUtxos
              + " more mustMixUtxo needed");
    }
    return missingMustMixUtxos;
  }
}
