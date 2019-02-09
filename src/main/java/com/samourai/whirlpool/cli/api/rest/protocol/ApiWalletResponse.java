package com.samourai.whirlpool.cli.api.rest.protocol;

import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;

public class ApiWalletResponse {
  private ApiWallet deposit;
  private ApiWallet premix;
  private ApiWallet postmix;

  public ApiWalletResponse(WhirlpoolWallet whirlpoolWallet) throws Exception {
    this.deposit = new ApiWallet(whirlpoolWallet.getUtxosDeposit());
    this.premix = new ApiWallet(whirlpoolWallet.getUtxosPremix());
    this.postmix = new ApiWallet(whirlpoolWallet.getUtxosPostmix());
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
