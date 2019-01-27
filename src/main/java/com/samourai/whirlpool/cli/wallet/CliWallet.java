package com.samourai.whirlpool.cli.wallet;

import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliWallet extends WhirlpoolWallet {
  private final Logger log = LoggerFactory.getLogger(CliWallet.class);

  public CliWallet(WhirlpoolWallet whirlpoolWallet) throws Exception {
    super(whirlpoolWallet);
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
