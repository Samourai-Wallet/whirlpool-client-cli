package com.samourai.whirlpool.cli.beans;

public class CliState {
  private CliStatus cliStatus;
  private String cliMessage;
  private boolean loggedIn;

  public CliState(CliStatus cliStatus, String cliMessage, boolean loggedIn) {
    this.cliStatus = cliStatus;
    this.cliMessage = cliMessage;
    this.loggedIn = loggedIn;
  }

  public CliStatus getCliStatus() {
    return cliStatus;
  }

  public String getCliMessage() {
    return cliMessage;
  }

  public boolean isLoggedIn() {
    return loggedIn;
  }
}
