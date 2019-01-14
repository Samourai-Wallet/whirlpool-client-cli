package com.samourai.whirlpool.client.run;

import com.samourai.api.client.SamouraiApi;
import com.samourai.api.client.beans.UnspentResponse;
import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.client.exception.BroadcastException;
import com.samourai.whirlpool.client.exception.EmptyWalletException;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.utils.CliUtils;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunLoopWallet {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int OUTPUTS_PER_TX0 = 5;

  private WhirlpoolClientConfig config;
  private SamouraiApi samouraiApi;
  private RunTx0 runTx0;
  private RunMixWallet runMixWallet;
  private Tx0Service tx0Service;
  private Bip84ApiWallet depositWallet;
  private Bip84ApiWallet premixWallet;
  private Optional<RunAggregateAndConsolidateWallet> runAggregateAndConsolidateWallet;

  public RunLoopWallet(
      WhirlpoolClientConfig config,
      SamouraiApi samouraiApi,
      RunTx0 runTx0,
      RunMixWallet runMixWallet,
      Tx0Service tx0Service,
      Bip84ApiWallet depositWallet,
      Bip84ApiWallet premixWallet,
      Optional<RunAggregateAndConsolidateWallet> runAggregateAndConsolidateWallet) {
    this.config = config;
    this.samouraiApi = samouraiApi;
    this.runTx0 = runTx0;
    this.runMixWallet = runMixWallet;
    this.tx0Service = tx0Service;
    this.depositWallet = depositWallet;
    this.premixWallet = premixWallet;
    this.runAggregateAndConsolidateWallet = runAggregateAndConsolidateWallet;
  }

  public boolean run(Pool pool, int nbClients) throws Exception {
    // fetch unspent utx0s
    log.info(" • Fetching unspent outputs from premix...");
    List<UnspentResponse.UnspentOutput> utxos = premixWallet.fetchUtxos();

    if (log.isDebugEnabled()) {
      log.debug("Found " + utxos.size() + " utxo from premix:");
      ClientUtils.logUtxos(utxos);
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
      int feeSatPerByte = samouraiApi.fetchFees();
      // not enough mustMixUtxos => Tx0
      for (int i = 0; i < missingMustMixUtxos; i++) {
        log.info(" • Tx0 (" + (i + 1) + "/" + missingMustMixUtxos + ")...");
        doRunTx0(pool, feeSatPerByte, missingMustMixUtxos);

        log.info("Refreshing utxos...");
        samouraiApi.refreshUtxos();
      }

      // recursive
      return run(pool, nbClients);
    } else {
      log.info(" • New mix...");
      return runMixWallet.runMix(mustMixUtxosUnique, pool);
    }
  }

  private void doRunTx0(Pool pool, int feeSatPerByte, int missingMustMixUtxos) throws Exception {
    try {
      runTx0.runTx0(pool, OUTPUTS_PER_TX0);
    } catch (BroadcastException e) {
      throw e;
    } catch (EmptyWalletException e) {
      // premixAndDeposit is empty => autoRefill when possible
      long missingBalance =
          tx0Service.computeSpendFromBalanceMin(pool, feeSatPerByte, missingMustMixUtxos);
      autoRefill(missingBalance);

      doRunTx0(pool, feeSatPerByte, missingMustMixUtxos);
    }
  }

  private void autoRefill(long missingBalance) throws Exception {
    String depositAddress =
        Bech32UtilGeneric.getInstance()
            .toBech32(depositWallet.getNextAddress(false), config.getNetworkParameters());
    String message =
        "depositWallet is empty. I need at least "
            + ClientUtils.satToBtc(missingBalance)
            + "btc (+ fees) to continue.\nPlease make a deposit to "
            + depositAddress;
    if (!runAggregateAndConsolidateWallet.isPresent()) {
      CliUtils.waitUserAction(message);
      return;
    }

    // auto aggregate postmix
    log.info(" • depositWallet wallet is empty. Aggregating postmix to refill it...");
    boolean aggregateSuccess = runAggregateAndConsolidateWallet.get().run();
    if (!aggregateSuccess) {
      CliUtils.waitUserAction(message);
    }

    log.info("Refreshing utxos...");
    samouraiApi.refreshUtxos();
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
