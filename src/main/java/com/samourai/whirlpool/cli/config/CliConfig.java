package com.samourai.whirlpool.cli.config;

import com.samourai.api.client.BackendServer;
import com.samourai.api.client.SamouraiApi;
import com.samourai.http.client.IHttpClient;
import com.samourai.stomp.client.IStompClientService;
import com.samourai.wallet.util.FormatsUtilGeneric;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.persist.WhirlpoolWalletPersistHandler;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CliConfig extends CliConfigFile {
  private boolean autoAggregatePostmix;
  private String autoTx0PoolId;

  public CliConfig() {
    super();
  }

  @Override
  public WhirlpoolWalletConfig computeWhirlpoolWalletConfig(
      IHttpClient httpClient,
      IStompClientService stompClientService,
      WhirlpoolWalletPersistHandler persistHandler,
      SamouraiApi samouraiApi) {

    // check valid
    if (autoAggregatePostmix && StringUtils.isEmpty(autoTx0PoolId)) {
      throw new RuntimeException("--auto-tx0 is required for --auto-aggregate-postmix");
    }

    WhirlpoolWalletConfig config =
        super.computeWhirlpoolWalletConfig(
            httpClient, stompClientService, persistHandler, samouraiApi);
    config.setAutoTx0PoolId(autoTx0PoolId);
    return config;
  }

  public String computeBackendUrl() {
    boolean isTestnet = FormatsUtilGeneric.getInstance().isTestNet(getServer().getParams());
    BackendServer backendServer = BackendServer.get(isTestnet);
    boolean useOnion = getTor() && getTorConfig().isOnionBackend();
    String backendUrl = backendServer.getBackendUrl(useOnion);
    return backendUrl;
  }

  public boolean isAutoAggregatePostmix() {
    return autoAggregatePostmix;
  }

  public void setAutoAggregatePostmix(boolean autoAggregatePostmix) {
    this.autoAggregatePostmix = autoAggregatePostmix;
  }

  public String getAutoTx0PoolId() {
    return autoTx0PoolId;
  }

  public void setAutoTx0PoolId(String autoTx0PoolId) {
    this.autoTx0PoolId = autoTx0PoolId;
  }

  @Override
  public Map<String, String> getConfigInfo() {
    Map<String, String> configInfo = super.getConfigInfo();

    configInfo.put("cli/version", Integer.toString(getVersion()));
    configInfo.put("cli/tor", Boolean.toString(getTor()));

    String apiKey = getApiKey();
    configInfo.put(
        "cli/apiKey",
        !org.apache.commons.lang3.StringUtils.isEmpty(apiKey)
            ? ClientUtils.maskString(apiKey)
            : "null");
    configInfo.put(
        "cli/proxy", getCliProxy().isPresent() ? getCliProxy().get().toString() : "null");
    configInfo.put("cli/autoAggregatePostmix", Boolean.toString(autoAggregatePostmix));
    configInfo.put("cli/autoTx0PoolId", autoTx0PoolId != null ? autoTx0PoolId : "null");
    return configInfo;
  }
}
