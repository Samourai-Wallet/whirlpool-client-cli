package com.samourai.whirlpool.cli.api.protocol.rest;

import com.samourai.whirlpool.cli.api.protocol.beans.ApiPool;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;
import java.util.stream.Collectors;

public class ApiTx0PoolsResponse {
  private Collection<ApiPool> pools;

  public ApiTx0PoolsResponse(Collection<Pool> pools) {
    this.pools = pools.stream().map(pool -> new ApiPool(pool)).collect(Collectors.toList());
  }

  public Collection<ApiPool> getPools() {
    return pools;
  }
}
