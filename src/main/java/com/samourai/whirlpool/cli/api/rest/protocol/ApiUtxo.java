package com.samourai.whirlpool.cli.api.rest.protocol;

import com.samourai.api.client.beans.UnspentResponse.UnspentOutput;
import com.samourai.whirlpool.client.wallet.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.WhirlpoolUtxoStatus;

public class ApiUtxo {
  private String hash;
  private int index;
  private long value;
  private int confirmations;
  private String path;
  private WhirlpoolUtxoStatus status;

  public ApiUtxo(WhirlpoolUtxo whirlpoolUtxo) {
    UnspentOutput utxo = whirlpoolUtxo.getUtxo();
    this.hash = utxo.tx_hash;
    this.index = utxo.tx_output_n;
    this.value = utxo.value;
    this.confirmations = utxo.confirmations;
    this.path = utxo.xpub.path;
    this.status = whirlpoolUtxo.getStatus();
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

  public WhirlpoolUtxoStatus getStatus() {
    return status;
  }
}
