package com.samourai.whirlpool.cli.api.protocol.rest;

import com.samourai.whirlpool.cli.api.protocol.beans.ApiPool;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;
import java.util.stream.Collectors;

public class ApiPoolsResponse {
  private Collection<ApiPool> pools;
  private Collection<ApiPool> poolsAvailable;

  public ApiPoolsResponse(
      Collection<Pool> pools,
      Collection<Pool> poolsAvailable,
      int feeSatPerByte,
      Tx0Service tx0Service) {
    this.pools =
        pools
            .stream()
            .map(pool -> computeApiPool(pool, feeSatPerByte, tx0Service))
            .collect(Collectors.toList());

    this.poolsAvailable =
        poolsAvailable
            .stream()
            .map(pool -> computeApiPool(pool, feeSatPerByte, tx0Service))
            .collect(Collectors.toList());
  }

  private ApiPool computeApiPool(Pool pool, int feeSatPerByte, Tx0Service tx0Service) {
    long tx0BalanceMin = tx0Service.computeSpendFromBalanceMin(pool, feeSatPerByte, 1);
    return new ApiPool(pool, tx0BalanceMin);
  }

  public Collection<ApiPool> getPools() {
    return pools;
  }

  public Collection<ApiPool> getPoolsAvailable() {
    return poolsAvailable;
  }
}
