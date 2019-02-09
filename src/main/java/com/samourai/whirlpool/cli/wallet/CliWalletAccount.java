package com.samourai.whirlpool.cli.wallet;

public enum CliWalletAccount {
  DEPOSIT(0),
  PREMIX(Integer.MAX_VALUE - 2),
  POSTMIX(Integer.MAX_VALUE - 1);

  private int accountIndex;

  CliWalletAccount(int accountIndex) {
    this.accountIndex = accountIndex;
  }

  public int getAccountIndex() {
    return accountIndex;
  }
}
