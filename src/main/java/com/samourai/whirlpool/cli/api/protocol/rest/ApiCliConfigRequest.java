package com.samourai.whirlpool.cli.api.protocol.rest;

import com.samourai.whirlpool.cli.api.protocol.beans.ApiCliConfig;

public class ApiCliConfigRequest {
  private ApiCliConfig config;

  public ApiCliConfigRequest() {}

  public ApiCliConfig getConfig() {
    return config;
  }

  public void setConfig(ApiCliConfig config) {
    this.config = config;
  }
}
