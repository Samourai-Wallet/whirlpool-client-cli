package com.samourai.whirlpool.cli.api.controllers;

import com.samourai.whirlpool.cli.api.protocol.CliApi;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.client.exception.NotifiableException;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

public abstract class AbstractRestController {
  @Autowired private CliConfig cliConfig;

  public AbstractRestController() {}

  protected void checkHeaders(HttpHeaders httpHeaders) throws Exception {
    // check apiVersion
    String requestApiVersion = httpHeaders.getFirst(CliApi.HEADER_API_VERSION);
    if (!Strings.isEmpty(requestApiVersion) && !CliApi.API_VERSION.equals(requestApiVersion)) {
      throw new NotifiableException(
          "API version mismatch: requestVersion="
              + requestApiVersion
              + ", cliVersion="
              + CliApi.API_VERSION);
    }

    // check apiKey
    if (!Strings.isEmpty(cliConfig.getApiKey())) {
      String requestApiKey = httpHeaders.getFirst(CliApi.HEADER_API_KEY);
      if (!cliConfig.getApiKey().equals(requestApiKey)) {
        throw new NotifiableException("API key rejected");
      }
    }
  }
}
