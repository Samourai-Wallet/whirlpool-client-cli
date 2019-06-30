package com.samourai.whirlpool.cli.services;

import com.samourai.stomp.client.IStompClient;
import com.samourai.stomp.client.IStompClientService;
import com.samourai.stomp.client.JavaStompClient;
import com.samourai.whirlpool.cli.config.CliConfig;
import org.springframework.stereotype.Service;

@Service
public class JavaStompClientService implements IStompClientService {
  private CliTorClientService torClientService;
  private CliConfig cliConfig;

  public JavaStompClientService(CliTorClientService torClientService, CliConfig cliConfig) {
    this.torClientService = torClientService;
    this.cliConfig = cliConfig;
  }

  @Override
  public IStompClient newStompClient() {
    return new JavaStompClient(torClientService, cliConfig);
  }
}
