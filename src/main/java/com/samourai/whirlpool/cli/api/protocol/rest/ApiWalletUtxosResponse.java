package com.samourai.whirlpool.cli.api.protocol.rest;

import com.samourai.whirlpool.cli.api.protocol.beans.ApiWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;

public class ApiWalletUtxosResponse {
  private ApiWallet deposit;
  private ApiWallet premix;
  private ApiWallet postmix;

  public ApiWalletUtxosResponse(WhirlpoolWallet whirlpoolWallet) throws Exception {
    this.deposit =
        new ApiWallet(whirlpoolWallet.getUtxosDeposit(), whirlpoolWallet.getZpubDeposit());
    this.premix = new ApiWallet(whirlpoolWallet.getUtxosPremix(), whirlpoolWallet.getZpubPremix());
    this.postmix =
        new ApiWallet(whirlpoolWallet.getUtxosPostmix(), whirlpoolWallet.getZpubPostmix());
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
