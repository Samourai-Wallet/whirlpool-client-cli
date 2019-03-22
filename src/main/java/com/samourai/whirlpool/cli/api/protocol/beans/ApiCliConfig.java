package com.samourai.whirlpool.cli.api.protocol.beans;

import com.samourai.whirlpool.cli.config.CliConfig;

public class ApiCliConfig {
  private String server;

  public ApiCliConfig() {}

  public ApiCliConfig(CliConfig cliConfig) {
    this.server = cliConfig.getServer().name();
  }

  public String getServer() {
    return server;
  }

  public void setServer(String server) {
    this.server = server;
  }
}
