package com.samourai.whirlpool.cli.api.protocol.rest;

import com.samourai.whirlpool.cli.beans.CliState;
import com.samourai.whirlpool.cli.beans.CliStatus;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;

public class ApiCliStateResponse {
  private CliStatus cliStatus;
  private String cliMessage;
  private boolean loggedIn;

  private String network;
  private String serverUrl;
  private String serverName;

  public ApiCliStateResponse(CliState cliState, WhirlpoolServer server) {
    this.cliStatus = cliState.getCliStatus();
    this.cliMessage = cliState.getCliMessage();
    this.loggedIn = cliState.isLoggedIn();

    this.network = server.getParams().getPaymentProtocolId();
    this.serverUrl = server.getServerUrl();
    this.serverName = server.name();
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

  public String getNetwork() {
    return network;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public String getServerName() {
    return serverName;
  }
}
