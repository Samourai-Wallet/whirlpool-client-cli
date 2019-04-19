package com.samourai.whirlpool.cli.services;

import com.samourai.stomp.client.JavaStompClient;
import com.samourai.whirlpool.cli.config.CliConfig;
import org.springframework.stereotype.Service;

@Service
public class JavaStompClientService extends JavaStompClient {

  public JavaStompClientService(CliTorClientService torClientService, CliConfig cliConfig) {
    super(torClientService, cliConfig);
  }
}
