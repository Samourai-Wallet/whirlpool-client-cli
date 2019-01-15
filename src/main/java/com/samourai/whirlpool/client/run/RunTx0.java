package com.samourai.whirlpool.client.run;

import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.wallet.CliWallet;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;

public class RunTx0 {
  private CliWallet cliWallet;

  public RunTx0(CliWallet cliWallet) {
    this.cliWallet = cliWallet;
  }

  public Tx0 runTx0(Pool pool, int nbOutputs) throws Exception {
    return cliWallet.tx0(pool, nbOutputs, 1);
  }
}
