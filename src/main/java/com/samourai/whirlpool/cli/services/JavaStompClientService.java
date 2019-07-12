package com.samourai.whirlpool.cli.services;

import com.samourai.stomp.client.IStompClient;
import com.samourai.stomp.client.IStompClientService;
import com.samourai.stomp.client.JavaStompClient;
import com.samourai.whirlpool.cli.config.CliConfig;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

@Service
public class JavaStompClientService implements IStompClientService {
  private CliTorClientService torClientService;
  private CliConfig cliConfig;
  private JavaHttpClientService httpClientService;

  private ThreadPoolTaskScheduler taskScheduler;

  public JavaStompClientService(
      CliTorClientService torClientService,
      CliConfig cliConfig,
      JavaHttpClientService httpClientService) {
    this.torClientService = torClientService;
    this.cliConfig = cliConfig;
    this.httpClientService = httpClientService;

    taskScheduler = new ThreadPoolTaskScheduler();
    taskScheduler.setPoolSize(1);
    taskScheduler.setThreadNamePrefix("stomp-heartbeat");
    taskScheduler.initialize();
  }

  @Override
  public IStompClient newStompClient() {
    return new JavaStompClient(torClientService, cliConfig, httpClientService, taskScheduler);
  }
}
