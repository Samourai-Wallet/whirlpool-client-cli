package com.samourai.whirlpool.cli.api.protocol.rest;

import com.samourai.whirlpool.cli.beans.CliState;
import com.samourai.whirlpool.cli.beans.CliStatus;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;

public class ApiCliStateResponse {
  private CliStatus cliStatus;
  private String cliMessage;
  private boolean loggedIn;
  private Integer torProgress;

  private String network;
  private String serverUrl;
  private String serverName;
  private String dojoUrl;
  private boolean tor;
  private boolean dojo;

  public ApiCliStateResponse(
      CliState cliState,
      WhirlpoolServer server,
      String serverUrl,
      String dojoUrl,
      boolean tor,
      boolean dojo) {
    this.cliStatus = cliState.getCliStatus();
    this.cliMessage = cliState.getCliMessage();
    this.loggedIn = cliState.isLoggedIn();
    this.torProgress = cliState.getTorProgress();

    this.network = server.getParams().getPaymentProtocolId();
    this.serverUrl = serverUrl;
    this.serverName = server.name();
    this.dojoUrl = dojoUrl;
    this.tor = tor;
    this.dojo = dojo;
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

  public String getNetwork() {
    return network;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public String getServerName() {
    return serverName;
  }

  public String getDojoUrl() {
    return dojoUrl;
  }

  public boolean isTor() {
    return tor;
  }

  public boolean isDojo() {
    return dojo;
  }
}
