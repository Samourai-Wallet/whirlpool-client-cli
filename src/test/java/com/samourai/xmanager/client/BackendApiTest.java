package com.samourai.xmanager.client;

import com.samourai.http.client.JavaHttpClient;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.test.AbstractTest;
import java.util.Optional;
import org.eclipse.jetty.client.HttpClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class BackendApiTest extends AbstractTest {
  private static final boolean testnet = true;
  private static final long requestTimeout = 5000;

  private BackendApi backendApi;

  public BackendApiTest() throws Exception {
    JavaHttpClient httpClient =
        new JavaHttpClient(requestTimeout) {
          @Override
          protected HttpClient computeHttpClient(boolean isRegisterOutput) throws Exception {
            return CliUtils.computeHttpClient(Optional.empty(), "whirlpool-cli/test");
          }
        };
    backendApi =
        new BackendApi(
            httpClient, BackendServer.TESTNET.getBackendUrlClear(), java8.util.Optional.empty());
  }

  @Disabled
  @Test
  public void initBip84() throws Exception {
    backendApi.initBip84("vpub...");
  }
}
