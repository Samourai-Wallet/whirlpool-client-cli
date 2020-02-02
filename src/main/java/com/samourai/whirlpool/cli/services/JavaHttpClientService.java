package com.samourai.whirlpool.cli.services;

import com.samourai.http.client.CliHttpClient;
import com.samourai.whirlpool.cli.config.CliConfig;
import org.springframework.stereotype.Service;

@Service
public class JavaHttpClientService extends CliHttpClient {

  public JavaHttpClientService(CliTorClientService torClientService, CliConfig cliConfig) {
    super(torClientService, cliConfig);
  }
}
