package com.samourai.whirlpool.cli.services;

import com.samourai.api.client.SamouraiApi;
import com.samourai.whirlpool.cli.config.CliConfig;
import org.springframework.stereotype.Service;

@Service
public class SamouraiApiService extends SamouraiApi {

  public SamouraiApiService(JavaHttpClientService httpClient, CliConfig cliConfig) {
    super(httpClient, cliConfig.computeBackendUrl());
  }
}
