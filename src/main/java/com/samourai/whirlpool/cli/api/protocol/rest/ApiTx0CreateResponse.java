package com.samourai.whirlpool.cli.api.protocol.rest;

public class ApiTx0CreateResponse {
  private String txid;

  public ApiTx0CreateResponse(String txid) {
    this.txid = txid;
  }

  public String getTxid() {
    return txid;
  }
}
