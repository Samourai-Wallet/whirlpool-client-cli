package com.samourai.whirlpool.cli.api.protocol.rest;

import javax.validation.constraints.NotEmpty;

public class ApiCliInitRequest {
  @NotEmpty public String pairingPayload;
  public boolean tor;
  public boolean dojo;

  public ApiCliInitRequest() {}
}
