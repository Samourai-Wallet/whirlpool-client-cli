package com.samourai.whirlpool.client.run;

import com.samourai.api.beans.UnspentResponse;
import com.samourai.whirlpool.client.utils.Bip84ApiWallet;
import com.samourai.whirlpool.client.utils.CliUtils;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunLoopWallet {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int MIN_MUST_MIX = 3;

  private RunTx0 runTx0;
  private Bip84ApiWallet depositAndPremixWallet;

  private RunMixWallet runMixWallet;

  public RunLoopWallet(
      RunTx0 runTx0, RunMixWallet runMixWallet, Bip84ApiWallet depositAndPremixWallet) {
    this.runTx0 = runTx0;
    this.runMixWallet = runMixWallet;
    this.depositAndPremixWallet = depositAndPremixWallet;
  }

  public boolean run(Pool pool) throws Exception {
    // fetch unspent utx0s
    log.info(" • Fetching unspent outputs from premix...");
    List<UnspentResponse.UnspentOutput> utxos =
        depositAndPremixWallet.fetchUtxos().stream().collect(Collectors.toList());
    if (!utxos.isEmpty()) {
      log.info("Found " + utxos.size() + " utxo from premix:");
      CliUtils.printUtxos(utxos);
    } else {
      log.error("ERROR: No utxo available from premix.");
      return false;
    }

    // find mustMixUtxos
    List<UnspentResponse.UnspentOutput> mustMixUtxos = CliUtils.filterUtxoMustMix(pool, utxos);
    log.info("Found " + mustMixUtxos.size() + " mustMixUtxo");

    // find liquidityUtxos
    List<UnspentResponse.UnspentOutput> liquidityUtxos = new ArrayList<>(); // TODO

    // how many utxos do we need for mix?
    int missingMustmixs = MIN_MUST_MIX - mustMixUtxos.size();
    int missingAnonymitySet =
        pool.getMixAnonymitySet() - (mustMixUtxos.size() + liquidityUtxos.size());
    log.info(
        "Next mix needs "
            + pool.getMixAnonymitySet()
            + " utxos (minMustMix="
            + MIN_MUST_MIX
            + " mustMix). I have "
            + mustMixUtxos.size()
            + " mustMixUtxo and "
            + liquidityUtxos.size()
            + " liquidityUtxo.");

    // do we have enough mustMixUtxo?
    int missingMustMixUtxos = Math.max(missingMustmixs, missingAnonymitySet);
    if (missingMustMixUtxos > 0) {
      // not enough mustMixUtxos => new Tx0
      log.info(
          " => I need " + missingMustMixUtxos + " more mustMixUtxo. Please broadcast a new Tx0.");

      // tx0
      log.info(" • Tx0...");
      runTx0.runTx0(pool, missingMustMixUtxos);
      return true;
    } else {
      log.info(" • New mix...");
      return runMixWallet.runMix(mustMixUtxos, pool);
    }
  }
}
