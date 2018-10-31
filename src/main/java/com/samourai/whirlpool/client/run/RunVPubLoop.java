package com.samourai.whirlpool.client.run;

import com.samourai.api.SamouraiApi;
import com.samourai.api.beans.UnspentResponse;
import com.samourai.whirlpool.client.CliUtils;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunVPubLoop {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int MIN_MUST_MIX = 3;

  public static final int ACCOUNT_DEPOSIT_AND_PREMIX = 0;
  public static final int CHAIN_DEPOSIT_AND_PREMIX = 0; // with change_index

  public static final int ACCOUNT_POSTMIX = Integer.MAX_VALUE;
  public static final int CHAIN_POSTMIX = 0; // with account_index

  private static final int SLEEP_LOOPS_SECONDS = 120;

  private WhirlpoolClientConfig config;
  private NetworkParameters params;
  private SamouraiApi samouraiApi;
  private RunTx0VPub runTx0VPub;
  private VpubWallet vpubWallet;

  private RunMixVPub runMixVPub;
  private IPostmixHandler postmixHandler;

  public RunVPubLoop(
      WhirlpoolClientConfig config,
      SamouraiApi samouraiApi,
      RunTx0VPub runTx0VPub,
      VpubWallet vpubWallet)
      throws Exception {
    this.config = config;
    this.params = config.getNetworkParameters();
    this.samouraiApi = samouraiApi;
    this.runTx0VPub = runTx0VPub;
    this.vpubWallet = vpubWallet;

    this.runMixVPub = new RunMixVPub(config);
    this.postmixHandler = runMixVPub.computePostmixHandler(vpubWallet, samouraiApi);
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
        vpubWallet
            .fetchUtxos(samouraiApi)
            .stream()
            .filter(utxo -> !isIgnoredUtxo(utxo))
            .collect(Collectors.toList());
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
      runTx0VPub.runTx0(utxos, vpubWallet, pool, missingMustMixUtxos);
    } else {
      log.info(" • New mix...");
      runMixVPub.runMix(mustMixUtxos, pool, vpubWallet, postmixHandler);
    }
  }

  private boolean isIgnoredUtxo(UnspentResponse.UnspentOutput utxo) {
    String[] ignores =
        new String[] {
          "96cc9f78b1df14c6baeb361e115e405862594a56dc82c060552b187a37d2050e",
          "79ed0dc5774843931c7d664ad9298b1139e8e4c2a08f7ec6333a0f1d6730d604",
          "ef67c991a728fc03ed904c20aafbd4bdc37a8264d221404b0fceea94da1b1dcf"
        };
    return ArrayUtils.contains(ignores, utxo.tx_hash);
  }
}
