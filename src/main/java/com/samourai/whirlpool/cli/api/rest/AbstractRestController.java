package com.samourai.whirlpool.cli.api.rest;

import com.samourai.whirlpool.cli.api.rest.protocol.CliApi;
import com.samourai.whirlpool.client.exception.NotifiableException;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpHeaders;

public abstract class AbstractRestController {
  public AbstractRestController() {}

  protected void checkHeaders(HttpHeaders httpHeaders) throws Exception {
    String requestApiVersion = httpHeaders.getFirst(CliApi.HEADER_API_VERSION);
    if (!Strings.isEmpty(requestApiVersion) && !CliApi.API_VERSION.equals(requestApiVersion)) {
      throw new NotifiableException(
          "API version mismatch: requestVersion="
              + requestApiVersion
              + ", cliVersion="
              + CliApi.API_VERSION);
    }
  }
}
