package com.samourai.whirlpool.cli.services;

import com.samourai.http.client.JavaHttpClient;
import com.samourai.whirlpool.cli.config.CliConfig;
import org.springframework.stereotype.Service;

@Service
public class JavaHttpClientService extends JavaHttpClient {

  public JavaHttpClientService(CliTorClientService torClientService, CliConfig cliConfig) {
    super(torClientService, cliConfig);
  }
}
