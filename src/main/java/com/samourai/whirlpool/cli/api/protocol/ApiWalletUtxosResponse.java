package com.samourai.whirlpool.cli.api.protocol;

import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;

public class ApiWalletUtxosResponse {
  private ApiWallet deposit;
  private ApiWallet premix;
  private ApiWallet postmix;

  public ApiWalletUtxosResponse(WhirlpoolWallet whirlpoolWallet) throws Exception {
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
