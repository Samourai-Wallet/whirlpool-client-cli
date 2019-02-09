package com.samourai.whirlpool.cli.wallet;

import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliWallet extends WhirlpoolWallet {
  private final Logger log = LoggerFactory.getLogger(CliWallet.class);

  public CliWallet(WhirlpoolWallet whirlpoolWallet) {
    super(whirlpoolWallet);
  }

  @Override
  public Bip84ApiWallet getWalletDeposit() {
    return super.getWalletDeposit();
  }

  @Override
  public Bip84ApiWallet getWalletPremix() {
    return super.getWalletPremix();
  }

  @Override
  public Bip84ApiWallet getWalletPostmix() {
    return super.getWalletPostmix();
  }
}
