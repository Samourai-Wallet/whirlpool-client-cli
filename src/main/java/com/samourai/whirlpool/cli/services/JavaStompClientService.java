package com.samourai.whirlpool.cli.services;

import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.JavaHttpClient;
import com.samourai.stomp.client.IStompClient;
import com.samourai.stomp.client.IStompClientService;
import com.samourai.stomp.client.JavaStompClient;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

@Service
public class JavaStompClientService implements IStompClientService {
  private JavaHttpClientService httpClientService;

  private ThreadPoolTaskScheduler taskScheduler;

  public JavaStompClientService(JavaHttpClientService httpClientService) {
    this.httpClientService = httpClientService;

    taskScheduler = new ThreadPoolTaskScheduler();
    taskScheduler.setPoolSize(1);
    taskScheduler.setThreadNamePrefix("stomp-heartbeat");
    taskScheduler.initialize();
  }

  @Override
  public IStompClient newStompClient() {
    JavaHttpClient httpClient = httpClientService.getHttpClient(HttpUsage.COORDINATOR_WEBSOCKET);
    return new JavaStompClient(httpClient, taskScheduler);
  }
}
