package com.samourai.whirlpool.client.run;

import com.samourai.api.beans.UnspentResponse;
import com.samourai.whirlpool.client.CliUtils;
import com.samourai.whirlpool.client.utils.Bip84ApiWallet;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunVPubLoop {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int MIN_MUST_MIX = 3;

  private static final int SLEEP_LOOPS_SECONDS = 120;

  private RunTx0VPub runTx0VPub;
  private Bip84ApiWallet depositAndPremixWallet;

  private RunMixVPub runMixVPub;

  public RunVPubLoop(
      RunTx0VPub runTx0VPub, RunMixVPub runMixVPub, Bip84ApiWallet depositAndPremixWallet) {
    this.runTx0VPub = runTx0VPub;
    this.runMixVPub = runMixVPub;
    this.depositAndPremixWallet = depositAndPremixWallet;
  }

  public void run(Pool pool) throws Exception {
    while (true) {
      log.info(" --------------------------------------- ");
      runLoop(pool);

      log.info(" => Next loop in " + SLEEP_LOOPS_SECONDS + " seconds...");
      Thread.sleep(SLEEP_LOOPS_SECONDS * 1000);
    }
  }

  public void runLoop(Pool pool) throws Exception {
    // fetch unspent utx0s
    log.info(" • Fetching unspent outputs from premix...");
    List<UnspentResponse.UnspentOutput> utxos =
        depositAndPremixWallet.fetchUtxos().stream().collect(Collectors.toList());
    if (!utxos.isEmpty()) {
      log.info("Found " + utxos.size() + " utxo from premix:");
      CliUtils.printUtxos(utxos);
    } else {
      log.error("ERROR: No utxo available from VPub.");
      return;
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
      runTx0VPub.runTx0(utxos, pool, missingMustMixUtxos);
    } else {
      log.info(" • New mix...");
      runMixVPub.runMix(mustMixUtxos, pool);
    }
  }
}
