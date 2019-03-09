package com.samourai.whirlpool.cli.api.protocol.rest;

import com.samourai.whirlpool.cli.beans.CliState;
import com.samourai.whirlpool.cli.beans.CliStatus;

public class ApiCliStateResponse {
  private CliStatus cliStatus;
  private String cliMessage;
  private boolean loggedIn;

  public ApiCliStateResponse(CliState cliState) {
    this.cliStatus = cliState.getCliStatus();
    this.cliMessage = cliState.getCliMessage();
    this.loggedIn = cliState.isLoggedIn();
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
