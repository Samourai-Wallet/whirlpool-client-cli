package com.samourai.whirlpool.cli.beans;

public class CliState {
  private CliStatus cliStatus;
  private String cliMessage;
  private boolean loggedIn;
  private Integer torProgress;

  public CliState(CliStatus cliStatus, String cliMessage, boolean loggedIn, Integer torProgress) {
    this.cliStatus = cliStatus;
    this.cliMessage = cliMessage;
    this.loggedIn = loggedIn;
    this.torProgress = torProgress;
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

  public Integer getTorProgress() {
    return torProgress;
  }
}
