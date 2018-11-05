package com.samourai.whirlpool.client.run;

import com.samourai.api.SamouraiApi;
import com.samourai.rpc.client.RpcClientService;
import com.samourai.whirlpool.client.utils.Bip84ApiWallet;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunAggregateAndConsolidateWallet {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int SLEEP_BEFORE_CONSOLIDATE = 10000;

  private NetworkParameters params;
  private SamouraiApi samouraiApi;
  private Optional<RpcClientService> rpcClientService;
  private Bip84ApiWallet depositAndPremixWallet;
  private Bip84ApiWallet postmixWallet;

  public RunAggregateAndConsolidateWallet(
      NetworkParameters params,
      SamouraiApi samouraiApi,
      Optional<RpcClientService> rpcClientService,
      Bip84ApiWallet depositAndPremixWallet,
      Bip84ApiWallet postmixWallet) {
    this.params = params;
    this.samouraiApi = samouraiApi;
    this.rpcClientService = rpcClientService;
    this.depositAndPremixWallet = depositAndPremixWallet;
    this.postmixWallet = postmixWallet;
  }

  public void run() throws Exception {
    // go aggregate postmix to premix
    log.info(" • Aggregating postmix wallet to premix...");
    new RunAggregateWallet(
            params, samouraiApi, rpcClientService, postmixWallet, depositAndPremixWallet)
        .run();

    // delay to let API detect the broadcasted tx
    log.info("Refreshing utxos, please wait...");
    synchronized (this) {
      Thread.sleep(SLEEP_BEFORE_CONSOLIDATE);
    }

    // consolidate premix
    log.info(" • Consolidating premix wallet...");
    new RunAggregateWallet(
            params, samouraiApi, rpcClientService, depositAndPremixWallet, depositAndPremixWallet)
        .run();
  }
}
