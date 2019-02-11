package com.samourai.whirlpool.cli.exception;

import com.samourai.whirlpool.client.exception.NotifiableException;

public class NoSessionWalletException extends NotifiableException {

  public NoSessionWalletException() {
    super("No wallet opened. Please open a wallet first");
  }
}
