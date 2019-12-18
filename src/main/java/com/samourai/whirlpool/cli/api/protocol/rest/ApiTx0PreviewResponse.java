package com.samourai.whirlpool.cli.api.protocol.rest;

import com.samourai.whirlpool.client.tx0.Tx0Preview;

public class ApiTx0PreviewResponse {
  private long minerFee;
  private long feeValue;
  private long feeChange;
  private long premixValue;
  private long changeValue;
  private int nbPremix;

  public ApiTx0PreviewResponse(Tx0Preview tx0Preview) {
    this.minerFee = tx0Preview.getMinerFee();
    this.feeValue = tx0Preview.getFeeValue();
    this.feeChange = tx0Preview.getFeeChange();
    this.premixValue = tx0Preview.getPremixValue();
    this.changeValue = tx0Preview.getChangeValue();
    this.nbPremix = tx0Preview.getNbPremix();
  }

  public long getMinerFee() {
    return minerFee;
  }

  public long getFeeValue() {
    return feeValue;
  }

  public long getFeeChange() {
    return feeChange;
  }

  public long getPremixValue() {
    return premixValue;
  }

  public long getChangeValue() {
    return changeValue;
  }

  public int getNbPremix() {
    return nbPremix;
  }
}
