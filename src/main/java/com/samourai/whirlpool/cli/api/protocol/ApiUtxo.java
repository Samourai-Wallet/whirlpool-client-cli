package com.samourai.whirlpool.cli.api.protocol;

import com.samourai.api.client.beans.UnspentResponse.UnspentOutput;
import com.samourai.whirlpool.client.wallet.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoStatus;

public class ApiUtxo {
  private String hash;
  private int index;
  private long value;
  private int confirmations;
  private String path;

  private WhirlpoolAccount account;
  private WhirlpoolUtxoStatus status;
  private Integer progressPercent;
  private String progressLabel;
  private String poolId;
  private int priority;
  private int mixsTarget;
  private int mixsDone;
  private String message;
  private String error;
  private Long lastActivityElapsed;

  public ApiUtxo(WhirlpoolUtxo whirlpoolUtxo) {
    UnspentOutput utxo = whirlpoolUtxo.getUtxo();
    this.hash = utxo.tx_hash;
    this.index = utxo.tx_output_n;
    this.value = utxo.value;
    this.confirmations = utxo.confirmations;
    this.path = utxo.xpub.path;

    this.account = whirlpoolUtxo.getAccount();
    this.status = whirlpoolUtxo.getStatus();
    this.progressPercent = whirlpoolUtxo.getProgressPercent();
    this.progressLabel = whirlpoolUtxo.getProgressLabel();
    this.poolId = whirlpoolUtxo.getPool() != null ? whirlpoolUtxo.getPool().getPoolId() : null;
    this.priority = whirlpoolUtxo.getPriority();
    this.mixsTarget = whirlpoolUtxo.getMixsTarget();
    this.mixsDone = whirlpoolUtxo.getMixsDone();
    this.message = whirlpoolUtxo.getMessage();
    this.error = whirlpoolUtxo.getError();
    this.lastActivityElapsed =
        whirlpoolUtxo.getLastActivity() != null
            ? System.currentTimeMillis() - whirlpoolUtxo.getLastActivity()
            : null;
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

  public WhirlpoolAccount getAccount() {
    return account;
  }

  public WhirlpoolUtxoStatus getStatus() {
    return status;
  }

  public Integer getProgressPercent() {
    return progressPercent;
  }

  public String getProgressLabel() {
    return progressLabel;
  }

  public String getPoolId() {
    return poolId;
  }

  public int getPriority() {
    return priority;
  }

  public int getMixsTarget() {
    return mixsTarget;
  }

  public int getMixsDone() {
    return mixsDone;
  }

  public String getMessage() {
    return message;
  }

  public String getError() {
    return error;
  }

  public Long getLastActivityElapsed() {
    return lastActivityElapsed;
  }
}
