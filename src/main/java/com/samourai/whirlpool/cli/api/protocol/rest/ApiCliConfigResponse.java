package com.samourai.whirlpool.cli.api.protocol.rest;

import com.samourai.whirlpool.cli.api.protocol.beans.ApiCliConfig;
import com.samourai.whirlpool.cli.config.CliConfig;

public class ApiCliConfigResponse {
  private ApiCliConfig config;

  public ApiCliConfigResponse(CliConfig cliConfig) {
    this.config = new ApiCliConfig(cliConfig);
  }

  public ApiCliConfig getConfig() {
    return config;
  }
}
