package com.samourai.whirlpool.cli.api.protocol.rest;

import com.samourai.whirlpool.client.tx0.Tx0;

public class ApiTx0Response extends ApiTx0PreviewResponse {
  private String txid;

  public ApiTx0Response(Tx0 tx0) {
    super(tx0);
    this.txid = tx0.getTx().getHashAsString();
  }

  public String getTxid() {
    return txid;
  }
}
