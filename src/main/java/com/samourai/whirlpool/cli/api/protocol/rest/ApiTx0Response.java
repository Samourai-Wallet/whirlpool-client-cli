package com.samourai.whirlpool.cli.api.protocol.rest;

public class ApiTx0Response {
  private String txid;

  public ApiTx0Response(String txid) {
    this.txid = txid;
  }

  public String getTxid() {
    return txid;
  }
}
