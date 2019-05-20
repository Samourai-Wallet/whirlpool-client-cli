package com.samourai.whirlpool.cli.services;

import com.samourai.api.client.SamouraiApi;
import com.samourai.http.client.IHttpClient;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.util.FormatsUtilGeneric;
import com.samourai.whirlpool.cli.config.CliConfig;
import org.springframework.stereotype.Service;

@Service
public class SamouraiApiService extends SamouraiApi {

  public SamouraiApiService(IHttpClient httpClient, CliConfig cliConfig) {
    super(
        httpClient,
        FormatsUtilGeneric.getInstance().isTestNet(cliConfig.getServer().getParams())
            ? BackendServer.TESTNET
            : BackendServer.MAINNET);
  }
}
