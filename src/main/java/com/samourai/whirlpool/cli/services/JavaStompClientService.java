package com.samourai.whirlpool.cli.services;

import com.samourai.stomp.client.JavaStompClient;
import org.springframework.stereotype.Service;

@Service
public class JavaStompClientService extends JavaStompClient {

  public JavaStompClientService(CliTorClientService torClientService) {
    super(torClientService);
  }
}
