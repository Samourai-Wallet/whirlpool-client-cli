package com.samourai.whirlpool.cli.api.protocol.rest;

import com.samourai.whirlpool.cli.beans.CliStatus;

public class ApiCliStatusResponse {
  private CliStatus cliStatus;
  private String cliMessage;
  private boolean loggedIn;

  public ApiCliStatusResponse(CliStatus cliStatus, String cliMessage, boolean loggedIn) {
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
