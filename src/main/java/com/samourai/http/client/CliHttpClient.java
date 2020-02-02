package com.samourai.http.client;

import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.services.CliTorClientService;
import com.samourai.whirlpool.cli.utils.CliUtils;
import java.lang.invoke.MethodHandles;
import org.eclipse.jetty.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliHttpClient extends JavaHttpClient {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private CliTorClientService torClientService;
  private CliConfig cliConfig;

  public CliHttpClient(CliTorClientService torClientService, CliConfig cliConfig) {
    super();
    this.torClientService = torClientService;
    this.cliConfig = cliConfig;
  }

  @Override
  protected HttpClient computeHttpClient(boolean isRegisterOutput) throws Exception {
    HttpClient httpClient =
        CliUtils.computeHttpClient(isRegisterOutput, torClientService, cliConfig.getCliProxy());
    return httpClient;
  }
}
