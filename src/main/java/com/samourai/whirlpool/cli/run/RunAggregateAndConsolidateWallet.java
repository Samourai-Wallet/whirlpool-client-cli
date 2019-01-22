package com.samourai.whirlpool.cli.run;

import com.samourai.api.client.SamouraiApi;
import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.whirlpool.client.wallet.pushTx.PushTxService;
import java.lang.invoke.MethodHandles;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunAggregateAndConsolidateWallet {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private NetworkParameters params;
  private SamouraiApi samouraiApi;
  private PushTxService pushTxService;
  private Bip84ApiWallet depositWallet;
  private Bip84ApiWallet premixWallet;
  private Bip84ApiWallet postmixWallet;

  public RunAggregateAndConsolidateWallet(
      NetworkParameters params,
      SamouraiApi samouraiApi,
      PushTxService pushTxService,
      Bip84ApiWallet depositWallet,
      Bip84ApiWallet premixWallet,
      Bip84ApiWallet postmixWallet) {
    this.params = params;
    this.samouraiApi = samouraiApi;
    this.pushTxService = pushTxService;
    this.depositWallet = depositWallet;
    this.premixWallet = premixWallet;
    this.postmixWallet = postmixWallet;
  }

  public boolean run() throws Exception {
    // consolidate postmix
    log.info(" • Consolidating postmix -> deposit...");
    new RunAggregateWallet(params, samouraiApi, pushTxService, postmixWallet).run(depositWallet);

    // consolidate premix
    log.info(" • Consolidating premix -> deposit...");
    new RunAggregateWallet(params, samouraiApi, pushTxService, premixWallet).run(depositWallet);

    // consolidate deposit
    log.info(" • Consolidating deposit...");
    boolean success =
        new RunAggregateWallet(params, samouraiApi, pushTxService, depositWallet)
            .run(depositWallet);
    return success;
  }
}
