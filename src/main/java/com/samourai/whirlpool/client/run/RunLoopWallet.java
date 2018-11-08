package com.samourai.whirlpool.client.run;

import com.samourai.api.SamouraiApi;
import com.samourai.api.beans.UnspentResponse;
import com.samourai.whirlpool.client.utils.Bip84ApiWallet;
import com.samourai.whirlpool.client.utils.CliUtils;
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

  private static final int MIN_MUST_MIX = 3;
  private static final int OUTPUTS_PER_TX0 = 10;

  private RunTx0 runTx0;
  private RunMixWallet runMixWallet;
  private Bip84ApiWallet depositAndPremixWallet;
  private Optional<RunAggregateAndConsolidateWallet> runAggregateAndConsolidateWallet;

  public RunLoopWallet(
      RunTx0 runTx0,
      RunMixWallet runMixWallet,
      Bip84ApiWallet depositAndPremixWallet,
      Optional<RunAggregateAndConsolidateWallet> runAggregateAndConsolidateWallet) {
    this.runTx0 = runTx0;
    this.runMixWallet = runMixWallet;
    this.depositAndPremixWallet = depositAndPremixWallet;
    this.runAggregateAndConsolidateWallet = runAggregateAndConsolidateWallet;
  }

  public boolean run(Pool pool, int nbClients) throws Exception {
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
        try {
          runTx0.runTx0(pool, OUTPUTS_PER_TX0);
        } catch (Exception e) {
          // premixAndDeposit is empty => autoRefill when possible
          autoRefill();
          runTx0.runTx0(pool, OUTPUTS_PER_TX0);
        }

        log.info("Refreshing utxos...");
        Thread.sleep(SamouraiApi.SLEEP_REFRESH_UTXOS);
      }

      // recursive
      return run(pool, nbClients);
    } else {
      log.info(" • New mix...");
      return runMixWallet.runMix(mustMixUtxosUnique, pool);
    }
  }

  private List<UnspentResponse.UnspentOutput> autoRefill() throws Exception {
    if (!runAggregateAndConsolidateWallet.isPresent()) {
      throw new Exception("ERROR: depositAndPremixWallet is empty.");
    }
    // auto aggregate postmix
    log.info(" • depositAndPremixWallet wallet is empty. Aggregating postmix to refill it...");
    runAggregateAndConsolidateWallet.get().run();

    log.info("Refreshing utxos...");
    Thread.sleep(SamouraiApi.SLEEP_REFRESH_UTXOS);

    List<UnspentResponse.UnspentOutput> utxos =
        depositAndPremixWallet.fetchUtxos().stream().collect(Collectors.toList());
    if (utxos.isEmpty()) {
      throw new Exception("ERROR: depositAndPremixWallet is empty.");
    }
    return utxos;
  }

  private int computeMissingMustMixUtxos(
      int nbClients,
      List<UnspentResponse.UnspentOutput> mustMixUtxos,
      List<UnspentResponse.UnspentOutput> liquidityUtxos) {
    int missingMustmixs = MIN_MUST_MIX - mustMixUtxos.size();
    int missingAnonymitySet = nbClients - (mustMixUtxos.size() + liquidityUtxos.size());
    int missingMustMixUtxos = Math.max(missingMustmixs, missingAnonymitySet);
    if (log.isDebugEnabled()) {
      log.debug(
          "Next mix needs "
              + nbClients
              + " utxos (minMustMix="
              + MIN_MUST_MIX
              + " mustMix). I have "
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
