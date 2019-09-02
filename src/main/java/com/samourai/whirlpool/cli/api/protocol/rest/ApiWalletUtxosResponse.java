package com.samourai.whirlpool.cli.api.protocol.rest;

import com.google.common.primitives.Ints;
import com.samourai.whirlpool.cli.api.protocol.beans.ApiWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import java.util.Comparator;
import java8.lang.Longs;

public class ApiWalletUtxosResponse {
  private ApiWallet deposit;
  private ApiWallet premix;
  private ApiWallet postmix;

  public ApiWalletUtxosResponse(WhirlpoolWallet whirlpoolWallet) throws Exception {
    Comparator<WhirlpoolUtxo> comparator =
        (o1, o2) -> {
          // last activity first
          if (o1.getLastActivity() != null || o2.getLastActivity() != null) {
            if (o1.getLastActivity() != null && o2.getLastActivity() == null) {
              return -1;
            }
            if (o2.getLastActivity() != null && o1.getLastActivity() == null) {
              return 1;
            }
            int compare = Longs.compare(o2.getLastActivity(), o1.getLastActivity());
            if (compare != 0) {
              return compare;
            }
          }

          // last confirmed
          return Ints.compare(o1.getUtxo().confirmations, o2.getUtxo().confirmations);
        };
    this.deposit =
        new ApiWallet(
            whirlpoolWallet.getUtxosDeposit(), whirlpoolWallet.getZpubDeposit(), comparator);
    this.premix =
        new ApiWallet(
            whirlpoolWallet.getUtxosPremix(), whirlpoolWallet.getZpubPremix(), comparator);
    this.postmix =
        new ApiWallet(
            whirlpoolWallet.getUtxosPostmix(), whirlpoolWallet.getZpubPostmix(), comparator);
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
