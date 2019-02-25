package com.samourai.whirlpool.cli.api.protocol.rest;

import com.samourai.whirlpool.cli.api.protocol.beans.ApiPool;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;
import java.util.stream.Collectors;

public class ApiPoolsResponse {
  private Collection<ApiPool> pools;

  public ApiPoolsResponse(
      Collection<Pool> pools, long feeValue, int feeSatPerByte, Tx0Service tx0Service) {
    this.pools =
        pools
            .stream()
            .map(
                pool -> {
                  long tx0BalanceMin =
                      tx0Service.computeSpendFromBalanceMin(pool, feeValue, feeSatPerByte, 1);
                  return new ApiPool(pool, tx0BalanceMin);
                })
            .collect(Collectors.toList());
  }

  public Collection<ApiPool> getPools() {
    return pools;
  }
}
