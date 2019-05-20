package com.samourai.whirlpool.cli.api.protocol.rest;

import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import javax.validation.constraints.NotNull;

public class ApiTx0Request {
  @NotNull public Tx0FeeTarget feeTarget;
  public String poolId;
  public Integer mixsTarget;

  public ApiTx0Request() {}
}
