package com.samourai.whirlpool.cli.api.protocol.rest;

import javax.validation.constraints.NotEmpty;

public class ApiCliLoginRequest {
  @NotEmpty public String seedPassphrase;

  public ApiCliLoginRequest() {}
}
