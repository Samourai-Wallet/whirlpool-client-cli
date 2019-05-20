package com.samourai.whirlpool.cli.api.protocol.rest;

import com.samourai.whirlpool.cli.api.protocol.beans.ApiCliConfig;
import javax.validation.constraints.NotNull;

public class ApiCliConfigRequest {
  @NotNull private ApiCliConfig config;

  public ApiCliConfigRequest() {}

  public ApiCliConfig getConfig() {
    return config;
  }

  public void setConfig(ApiCliConfig config) {
    this.config = config;
  }
}
