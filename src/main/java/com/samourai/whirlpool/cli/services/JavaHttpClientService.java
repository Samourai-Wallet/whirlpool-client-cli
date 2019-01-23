package com.samourai.whirlpool.cli.services;

import com.samourai.http.client.JavaHttpClient;
import org.springframework.stereotype.Service;

@Service
public class JavaHttpClientService extends JavaHttpClient {

  public JavaHttpClientService(CliTorClientService torClientService) {
    super(torClientService);
  }
}
