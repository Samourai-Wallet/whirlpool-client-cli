package com.samourai.whirlpool.cli.api.rest.protocol;

import com.samourai.whirlpool.cli.wallet.CliWallet;

public class ApiWalletResponse {
  private ApiWallet deposit;
  private ApiWallet premix;
  private ApiWallet postmix;

  public ApiWalletResponse(CliWallet cliWallet) throws Exception {
    this.deposit = new ApiWallet(cliWallet.getUtxosDeposit());
    this.premix = new ApiWallet(cliWallet.getUtxosPremix());
    this.postmix = new ApiWallet(cliWallet.getUtxosPostmix());
  }

  public ApiWallet getDeposit() {
    return deposit;
  }

  public ApiWallet getPremix() {
    return premix;
  }

  public ApiWallet getPostmix() {
    return postmix;
  }
}
