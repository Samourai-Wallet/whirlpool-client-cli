package com.samourai.whirlpool.cli.api.protocol.rest;

public class ApiCliInitResponse {
  private String apiKey;

  public ApiCliInitResponse(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getApiKey() {
    return apiKey;
  }
}
