package com.samourai.whirlpool.cli.api.protocol.rest;

import com.samourai.whirlpool.cli.beans.CliStatus;

public class ApiCliStatusResponse {
  private CliStatus cliStatus;
  private boolean loggedIn;

  public ApiCliStatusResponse(CliStatus cliStatus, boolean loggedIn) {
    this.cliStatus = cliStatus;
    this.loggedIn = loggedIn;
  }

  public CliStatus getCliStatus() {
    return cliStatus;
  }

  public boolean isLoggedIn() {
    return loggedIn;
  }
}
