package com.samourai.whirlpool.cli.config;

import com.samourai.http.client.HttpUsage;
import com.samourai.http.client.IHttpClientService;
import com.samourai.stomp.client.IStompClientService;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.BackendServer;
import com.samourai.wallet.util.FormatsUtilGeneric;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.persist.WhirlpoolWalletPersistHandler;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
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
      IHttpClientService httpClientService,
      IStompClientService stompClientService,
      WhirlpoolWalletPersistHandler persistHandler,
      BackendApi backendApi) {

    // check valid
    if (autoAggregatePostmix && StringUtils.isEmpty(autoTx0PoolId)) {
      throw new RuntimeException("--auto-tx0 is required for --auto-aggregate-postmix");
    }

    WhirlpoolWalletConfig config =
        super.computeWhirlpoolWalletConfig(
            httpClientService, stompClientService, persistHandler, backendApi);
    config.setAutoTx0PoolId(autoTx0PoolId);
    return config;
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

  //

  public String computeBackendUrl() {
    if (getDojo().isEnabled()) {
      // use dojo
      return getDojo().getUrl();
    }
    // use Samourai backend
    return computeBackendUrlSamourai();
  }

  public boolean isDojoEnabled() {
    return getDojo() != null && getDojo().isEnabled();
  }

  public String computeBackendApiKey() {
    if (isDojoEnabled()) {
      // dojo: use apiKey
      return getDojo().getApiKey();
    }
    // Samourai backend: no apiKey
    return null;
  }

  private String computeBackendUrlSamourai() {
    boolean isTestnet = FormatsUtilGeneric.getInstance().isTestNet(getServer().getParams());
    BackendServer backendServer = BackendServer.get(isTestnet);
    boolean useOnion =
        getTor()
            && getTorConfig().getBackend().isEnabled()
            && getTorConfig().getBackend().isOnion();
    String backendUrl = backendServer.getBackendUrl(useOnion);
    return backendUrl;
  }

  public Collection<HttpUsage> computeTorHttpUsages() {
    List<HttpUsage> httpUsages = new LinkedList<>();
    if (!getTor()) {
      // tor is disabled
      return httpUsages;
    }

    // backend
    if (getTorConfig().getBackend().isEnabled()) {
      httpUsages.add(HttpUsage.BACKEND);
    }

    // coordinator
    if (getTorConfig().getCoordinator().isEnabled()) {
      httpUsages.add(HttpUsage.COORDINATOR_WEBSOCKET);
      httpUsages.add(HttpUsage.COORDINATOR_REST);
      httpUsages.add(HttpUsage.COORDINATOR_REGISTER_OUTPUT);
    }
    return httpUsages;
  }
}
