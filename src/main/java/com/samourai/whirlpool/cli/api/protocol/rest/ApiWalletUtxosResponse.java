package com.samourai.whirlpool.cli.api.protocol.rest;

import com.google.common.primitives.Ints;
import com.samourai.whirlpool.cli.api.protocol.beans.ApiWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoState;
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
          WhirlpoolUtxoState s1 = o1.getUtxoState();
          WhirlpoolUtxoState s2 = o2.getUtxoState();
          if (s1.getLastActivity() != null || s2.getLastActivity() != null) {
            if (s1.getLastActivity() != null && s2.getLastActivity() == null) {
              return -1;
            }
            if (s2.getLastActivity() != null && s1.getLastActivity() == null) {
              return 1;
            }
            int compare = Longs.compare(s2.getLastActivity(), s1.getLastActivity());
            if (compare != 0) {
              return compare;
            }
          }

          // last confirmed
          return Ints.compare(o1.getUtxo().confirmations, o2.getUtxo().confirmations);
        };
    int mixsTargetMin = whirlpoolWallet.getConfig().getMixsTarget();
    this.deposit =
        new ApiWallet(
            whirlpoolWallet.getUtxosDeposit(),
            whirlpoolWallet.getZpubDeposit(),
            comparator,
            mixsTargetMin);
    this.premix =
        new ApiWallet(
            whirlpoolWallet.getUtxosPremix(),
            whirlpoolWallet.getZpubPremix(),
            comparator,
            mixsTargetMin);
    this.postmix =
        new ApiWallet(
            whirlpoolWallet.getUtxosPostmix(),
            whirlpoolWallet.getZpubPostmix(),
            comparator,
            mixsTargetMin);
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
