package com.samourai.whirlpool.cli.api.protocol.rest;

import com.samourai.whirlpool.cli.api.protocol.beans.ApiPool;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class ApiPoolsResponse {
  private Collection<ApiPool> pools;

  public ApiPoolsResponse(
      Collection<Pool> pools,
      Tx0FeeTarget feeTarget,
      WhirlpoolWallet whirlpoolWallet,
      Map<String, Long> overspendPerPool) {
    this.pools =
        pools
            .stream()
            .map(
                pool -> {
                  Long overspendOrNull =
                      overspendPerPool != null ? overspendPerPool.get(pool.getPoolId()) : null;
                  return computeApiPool(pool, feeTarget, whirlpoolWallet, overspendOrNull);
                })
            .collect(Collectors.toList());
  }

  private ApiPool computeApiPool(
      Pool pool, Tx0FeeTarget feeTarget, WhirlpoolWallet whirlpoolWallet, Long overspendOrNull) {
    long tx0BalanceMin =
        whirlpoolWallet.computeTx0SpendFromBalanceMin(pool, feeTarget, 1, overspendOrNull);
    return new ApiPool(pool, tx0BalanceMin);
  }

  public Collection<ApiPool> getPools() {
    return pools;
  }
}
