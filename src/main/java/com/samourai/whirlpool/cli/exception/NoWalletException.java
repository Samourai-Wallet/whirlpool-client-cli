package com.samourai.whirlpool.cli.exception;

import com.samourai.whirlpool.client.exception.NotifiableException;

public class NoWalletException extends NotifiableException {

  public NoWalletException() {
    super("No wallet opened. Please open a wallet first");
  }
}
