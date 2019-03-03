package com.samourai.whirlpool.cli.api.protocol.rest;

import com.samourai.whirlpool.cli.beans.CliStatus;

public class ApiCliStatusResponse {
  private CliStatus cliStatus;

  public ApiCliStatusResponse(CliStatus cliStatus) {
    this.cliStatus = cliStatus;
  }

  public CliStatus getCliStatus() {
    return cliStatus;
  }
}
