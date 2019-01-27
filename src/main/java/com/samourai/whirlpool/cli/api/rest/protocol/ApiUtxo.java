package com.samourai.whirlpool.cli.api.rest.protocol;

import com.samourai.api.client.beans.UnspentResponse.UnspentOutput;

public class ApiUtxo {
  private String hash;
  private int index;
  private long value;
  private int confirmations;
  private String path;

  public ApiUtxo(UnspentOutput utxo) {
    this(utxo.tx_hash, utxo.tx_output_n, utxo.value, utxo.confirmations, utxo.xpub.path);
  }

  public ApiUtxo(String hash, int index, long value, int confirmations, String path) {
    this.hash = hash;
    this.index = index;
    this.value = value;
    this.confirmations = confirmations;
    this.path = path;
  }

  public String getHash() {
    return hash;
  }

  public int getIndex() {
    return index;
  }

  public long getValue() {
    return value;
  }

  public int getConfirmations() {
    return confirmations;
  }

  public String getPath() {
    return path;
  }
}
