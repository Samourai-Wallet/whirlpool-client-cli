package com.samourai.whirlpool.client.exception;

import org.bitcoinj.core.Transaction;

public class BroadcastException extends Exception {
  private Transaction tx;

  public BroadcastException(Transaction tx) {
    this.tx = tx;
  }

  public Transaction getTx() {
    return tx;
  }
}
