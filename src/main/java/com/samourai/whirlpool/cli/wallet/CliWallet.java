package com.samourai.whirlpool.cli.wallet;

import com.samourai.api.client.SamouraiApi;
import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.pushTx.PushTxService;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliWallet extends WhirlpoolWallet {
  private final Logger log = LoggerFactory.getLogger(CliWallet.class);

  public CliWallet(
      NetworkParameters params,
      SamouraiApi samouraiApi,
      PushTxService pushTxService,
      Tx0Service tx0Service,
      WhirlpoolClient whirlpoolClient,
      IIndexHandler feeIndexHandler,
      Bip84ApiWallet depositWallet,
      Bip84ApiWallet premixWallet,
      Bip84ApiWallet postmixWallet)
      throws Exception {
    super(
        params,
        samouraiApi,
        pushTxService,
        tx0Service,
        whirlpoolClient,
        feeIndexHandler,
        depositWallet,
        premixWallet,
        postmixWallet);
  }

  @Override
  public Bip84ApiWallet getDepositWallet() {
    return super.getDepositWallet();
  }

  @Override
  public Bip84ApiWallet getPremixWallet() {
    return super.getPremixWallet();
  }

  @Override
  public Bip84ApiWallet getPostmixWallet() {
    return super.getPostmixWallet();
  }
}
