package com.samourai.whirlpool.cli.api.protocol.beans;

import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.websocket.notifications.MixStatus;

public class ApiPool {
  private String poolId;
  private long denomination;
  private long minerFeeMin;
  private long minerFeeMax;
  private int minAnonymitySet;
  private int nbRegistered;
  private int mixAnonymitySet;
  private MixStatus mixStatus;
  private long elapsedTime;
  private int mixNbConfirmed;

  public ApiPool() {}

  public ApiPool(Pool pool) {
    this.poolId = pool.getPoolId();
    this.denomination = pool.getDenomination();
    this.minerFeeMin = pool.getMinerFeeMin();
    this.minerFeeMax = pool.getMinerFeeMax();
    this.minAnonymitySet = pool.getMinAnonymitySet();
    this.nbRegistered = pool.getNbRegistered();
    this.mixAnonymitySet = pool.getMixAnonymitySet();
    this.mixStatus = pool.getMixStatus();
    this.elapsedTime = pool.getElapsedTime();
    this.mixNbConfirmed = pool.getMixNbConfirmed();
  }

  public String getPoolId() {
    return poolId;
  }

  public long getDenomination() {
    return denomination;
  }

  public long getMinerFeeMin() {
    return minerFeeMin;
  }

  public long getMinerFeeMax() {
    return minerFeeMax;
  }

  public int getMinAnonymitySet() {
    return minAnonymitySet;
  }

  public int getNbRegistered() {
    return nbRegistered;
  }

  public int getMixAnonymitySet() {
    return mixAnonymitySet;
  }

  public MixStatus getMixStatus() {
    return mixStatus;
  }

  public long getElapsedTime() {
    return elapsedTime;
  }

  public int getMixNbConfirmed() {
    return mixNbConfirmed;
  }
}
