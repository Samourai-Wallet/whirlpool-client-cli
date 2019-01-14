package com.samourai.whirlpool.client.run;

import com.samourai.api.client.SamouraiApi;
import com.samourai.rpc.client.RpcClientService;
import com.samourai.wallet.client.Bip84ApiWallet;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunAggregateAndConsolidateWallet {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private NetworkParameters params;
  private SamouraiApi samouraiApi;
  private Optional<RpcClientService> rpcClientService;
  private Bip84ApiWallet depositWallet;
  private Bip84ApiWallet premixWallet;
  private Bip84ApiWallet postmixWallet;

  public RunAggregateAndConsolidateWallet(
      NetworkParameters params,
      SamouraiApi samouraiApi,
      Optional<RpcClientService> rpcClientService,
      Bip84ApiWallet depositWallet,
      Bip84ApiWallet premixWallet,
      Bip84ApiWallet postmixWallet) {
    this.params = params;
    this.samouraiApi = samouraiApi;
    this.rpcClientService = rpcClientService;
    this.depositWallet = depositWallet;
    this.premixWallet = premixWallet;
    this.postmixWallet = postmixWallet;
  }

  public boolean run() throws Exception {
    // consolidate postmix
    log.info(" • Consolidating postmix -> deposit...");
    new RunAggregateWallet(params, samouraiApi, rpcClientService, postmixWallet).run(depositWallet);

    // consolidate premix
    log.info(" • Consolidating premix -> deposit...");
    new RunAggregateWallet(params, samouraiApi, rpcClientService, premixWallet).run(depositWallet);

    // consolidate deposit
    log.info(" • Consolidating deposit...");
    boolean success =
        new RunAggregateWallet(params, samouraiApi, rpcClientService, depositWallet)
            .run(depositWallet);
    return success;
  }
}
