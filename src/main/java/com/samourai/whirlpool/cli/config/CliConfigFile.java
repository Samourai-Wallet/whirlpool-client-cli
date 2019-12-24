package com.samourai.whirlpool.cli.config;

import com.samourai.http.client.IHttpClient;
import com.samourai.stomp.client.IStompClientService;
import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.whirlpool.cli.beans.CliProxy;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import com.samourai.whirlpool.client.wallet.persist.WhirlpoolWalletPersistHandler;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.NotEmpty;
import org.apache.logging.log4j.util.Strings;
import org.bitcoinj.core.NetworkParameters;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "cli")
@Configuration
public abstract class CliConfigFile {
  private int version; // 0 for versions < 1
  private WhirlpoolServer server;
  private String scode;
  @NotEmpty private boolean tor;
  @NotEmpty private TorConfig torConfig;
  @NotEmpty private DojoConfig dojo;
  @NotEmpty private String apiKey;
  @NotEmpty private String seed;
  @NotEmpty private boolean seedAppendPassphrase;
  @NotEmpty private int persistDelay;
  @NotEmpty private int refreshPoolsDelay;
  @NotEmpty private int tx0MinConfirmations;
  @NotEmpty private String proxy;
  private Optional<CliProxy> _cliProxy;
  @NotEmpty private MixConfig mix;
  @NotEmpty private ApiConfig api;

  private static final String PUSHTX_AUTO = "auto";
  private static final String PUSHTX_INTERACTIVE = "interactive";

  public CliConfigFile() {
    // warning: properties are NOT loaded yet
    // it will be loaded later on SpringBoot application run()
  }

  public CliConfigFile(CliConfigFile copy) {
    this.version = copy.version;
    this.server = copy.server;
    this.scode = copy.scode;
    this.tor = copy.tor;
    this.torConfig = new TorConfig(copy.torConfig);
    this.dojo = new DojoConfig(copy.dojo);
    this.apiKey = copy.apiKey;
    this.seed = copy.seed;
    this.seedAppendPassphrase = copy.seedAppendPassphrase;
    this.persistDelay = copy.persistDelay;
    this.refreshPoolsDelay = copy.refreshPoolsDelay;
    this.tx0MinConfirmations = copy.tx0MinConfirmations;
    this.proxy = copy.proxy;
    this.api = new ApiConfig(copy.api);
    this.mix = new MixConfig(copy.mix);
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public WhirlpoolServer getServer() {
    return server;
  }

  public void setServer(WhirlpoolServer server) {
    this.server = server;
  }

  public String getScode() {
    return scode;
  }

  public void setScode(String scode) {
    this.scode = scode;
  }

  public boolean getTor() {
    return tor;
  }

  public void setTor(boolean tor) {
    this.tor = tor;
  }

  public TorConfig getTorConfig() {
    return torConfig;
  }

  public void setTorConfig(TorConfig torConfig) {
    this.torConfig = torConfig;
  }

  public DojoConfig getDojo() {
    return dojo;
  }

  public void setDojo(DojoConfig dojo) {
    this.dojo = dojo;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getSeed() {
    return seed;
  }

  public void setSeed(String seed) {
    this.seed = seed;
  }

  public boolean isSeedAppendPassphrase() {
    return seedAppendPassphrase;
  }

  public void setSeedAppendPassphrase(boolean seedAppendPassphrase) {
    this.seedAppendPassphrase = seedAppendPassphrase;
  }

  public int getPersistDelay() {
    return persistDelay;
  }

  public void setPersistDelay(int persistDelay) {
    this.persistDelay = persistDelay;
  }

  public int getRefreshPoolsDelay() {
    return refreshPoolsDelay;
  }

  public void setRefreshPoolsDelay(int refreshPoolsDelay) {
    this.refreshPoolsDelay = refreshPoolsDelay;
  }

  public int getTx0MinConfirmations() {
    return tx0MinConfirmations;
  }

  public void setTx0MinConfirmations(int tx0MinConfirmations) {
    this.tx0MinConfirmations = tx0MinConfirmations;
  }

  public String getProxy() {
    return proxy;
  }

  public Optional<CliProxy> getCliProxy() {
    if (_cliProxy == null) {
      _cliProxy = CliUtils.computeProxy(proxy);
    }
    return _cliProxy;
  }

  public void setProxy(String proxy) {
    this.proxy = proxy;
  }

  public MixConfig getMix() {
    return mix;
  }

  public void setMix(MixConfig mix) {
    this.mix = mix;
  }

  public ApiConfig getApi() {
    return api;
  }

  public void setApi(ApiConfig api) {
    this.api = api;
  }

  public static class MixConfig {
    @NotEmpty private Integer clients;
    @NotEmpty private int clientsPerPool;
    @NotEmpty private int clientDelay;
    @NotEmpty private int tx0Delay;
    @NotEmpty private int tx0MaxOutputs;
    @NotEmpty private boolean autoMix;
    @NotEmpty private int mixsTarget;

    public MixConfig() {}

    public MixConfig(MixConfig copy) {
      this.clients = copy.clients;
      this.clientsPerPool = copy.clientsPerPool;
      this.clientDelay = copy.clientDelay;
      this.tx0Delay = copy.tx0Delay;
      this.tx0MaxOutputs = copy.tx0MaxOutputs;
      this.autoMix = copy.autoMix;
      this.mixsTarget = copy.mixsTarget;
    }

    public Integer getClients() {
      return clients;
    }

    public void setClients(Integer clients) {
      this.clients = clients;
    }

    public int getClientsPerPool() {
      return clientsPerPool;
    }

    public void setClientsPerPool(int clientsPerPool) {
      this.clientsPerPool = clientsPerPool;
    }

    public int getClientDelay() {
      return clientDelay;
    }

    public void setClientDelay(int clientDelay) {
      this.clientDelay = clientDelay;
    }

    public int getTx0Delay() {
      return tx0Delay;
    }

    public void setTx0Delay(int tx0Delay) {
      this.tx0Delay = tx0Delay;
    }

    public int getTx0MaxOutputs() {
      return tx0MaxOutputs;
    }

    public void setTx0MaxOutputs(int tx0MaxOutputs) {
      this.tx0MaxOutputs = tx0MaxOutputs;
    }

    public boolean isAutoMix() {
      return autoMix;
    }

    public void setAutoMix(boolean autoMix) {
      this.autoMix = autoMix;
    }

    public int getMixsTarget() {
      return mixsTarget;
    }

    public void setMixsTarget(int mixsTarget) {
      this.mixsTarget = mixsTarget;
    }

    public Map<String, String> getConfigInfo() {
      Map<String, String> configInfo = new HashMap<>();
      configInfo.put("cli/mix/clients", clients != null ? Integer.toString(clients) : "null");
      configInfo.put("cli/mix/clientsPerPool", Integer.toString(clientsPerPool));
      configInfo.put("cli/mix/clientDelay", Integer.toString(clientDelay));
      configInfo.put("cli/mix/tx0Delay", Integer.toString(tx0Delay));
      configInfo.put("cli/mix/tx0MaxOutputs", Integer.toString(tx0MaxOutputs));
      configInfo.put("cli/mix/autoMix", Boolean.toString(autoMix));
      configInfo.put("cli/mix/mixsTarget", Integer.toString(mixsTarget));
      return configInfo;
    }
  }

  public static class ApiConfig {
    @NotEmpty private int portHttps;
    @NotEmpty private int portHttp;
    @NotEmpty private boolean requireHttps;

    public ApiConfig() {}

    public ApiConfig(ApiConfig copy) {
      this.portHttps = copy.portHttps;
      this.portHttp = copy.portHttp;
      this.requireHttps = copy.requireHttps;
    }

    public int getPortHttps() {
      return portHttps;
    }

    public void setPortHttps(int portHttps) {
      this.portHttps = portHttps;
    }

    public int getPortHttp() {
      return portHttp;
    }

    public void setPortHttp(int portHttp) {
      this.portHttp = portHttp;
    }

    public boolean isRequireHttps() {
      return requireHttps;
    }

    public void setRequireHttps(boolean requireHttps) {
      this.requireHttps = requireHttps;
    }

    public Map<String, String> getConfigInfo() {
      Map<String, String> configInfo = new HashMap<>();
      configInfo.put(
          "cli/api",
          "portHttps="
              + portHttps
              + ", portHttp="
              + portHttp
              + ", requireHttps="
              + Boolean.toString(requireHttps));
      return configInfo;
    }
  }

  public static class TorConfig {
    public static final String EXECUTABLE_AUTO = "auto";
    public static final String EXECUTABLE_LOCAL = "local";
    @NotEmpty private String executable;
    @NotEmpty private boolean onionServer;
    @NotEmpty private boolean onionBackend;

    public TorConfig() {}

    public TorConfig(TorConfig copy) {
      this.executable = copy.executable;
      this.onionServer = copy.onionServer;
      this.onionBackend = copy.onionBackend;
    }

    public String getExecutable() {
      return executable;
    }

    public void setExecutable(String executable) {
      this.executable = executable;
    }

    public boolean isExecutableAuto() {
      return EXECUTABLE_AUTO.equals(this.executable);
    }

    public boolean isExecutableLocal() {
      return EXECUTABLE_LOCAL.equals(this.executable);
    }

    public boolean isOnionServer() {
      return onionServer;
    }

    public void setOnionServer(boolean onionServer) {
      this.onionServer = onionServer;
    }

    public boolean isOnionBackend() {
      return onionBackend;
    }

    public void setOnionBackend(boolean onionBackend) {
      this.onionBackend = onionBackend;
    }

    public Map<String, String> getConfigInfo() {
      Map<String, String> configInfo = new HashMap<>();
      configInfo.put("cli/tor/executable", executable);
      configInfo.put("cli/tor/onionServer", Boolean.toString(onionServer));
      configInfo.put("cli/tor/onionBackend", Boolean.toString(onionBackend));
      return configInfo;
    }
  }

  public static class DojoConfig {
    @NotEmpty private String url;
    @NotEmpty private String apiKey;
    @NotEmpty private boolean enabled;

    public DojoConfig() {}

    public DojoConfig(DojoConfig copy) {
      this.url = copy.url;
      this.apiKey = copy.apiKey;
      this.enabled = copy.enabled;
    }

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Map<String, String> getConfigInfo() {
      Map<String, String> configInfo = new HashMap<>();
      configInfo.put("cli/dojo/url", url);
      configInfo.put("cli/dojo/apiKey", ClientUtils.maskString(apiKey));
      configInfo.put("cli/dojo/enabled", Boolean.toString(enabled));
      return configInfo;
    }
  }

  public String computeServerUrl() {
    boolean useOnion = tor && torConfig.onionServer;
    String serverUrl = server.getServerUrl(useOnion);
    return serverUrl;
  }

  protected WhirlpoolWalletConfig computeWhirlpoolWalletConfig(
      IHttpClient httpClient,
      IStompClientService stompClientService,
      WhirlpoolWalletPersistHandler persistHandler,
      BackendApi backendApi) {
    String serverUrl = computeServerUrl();
    NetworkParameters params = server.getParams();
    WhirlpoolWalletConfig config =
        new WhirlpoolWalletConfig(
            httpClient, stompClientService, persistHandler, serverUrl, params, false, backendApi);
    if (!Strings.isEmpty(scode)) {
      config.setScode(scode);
    }
    config.setPersistDelay(persistDelay);
    config.setRefreshPoolsDelay(refreshPoolsDelay);
    config.setTx0MinConfirmations(tx0MinConfirmations);

    config.setMaxClients(mix.getClients());
    config.setMaxClientsPerPool(mix.getClientsPerPool());
    config.setClientDelay(mix.getClientDelay());
    config.setTx0Delay(mix.getTx0Delay());
    config.setTx0MaxOutputs(mix.getTx0MaxOutputs() > 0 ? mix.getTx0MaxOutputs() : null);
    config.setAutoMix(mix.isAutoMix());
    config.setMixsTarget(mix.getMixsTarget());

    return config;
  }

  public Map<String, String> getConfigInfo() {
    Map<String, String> configInfo = new LinkedHashMap<>();
    configInfo.put("cli/server", server.name());
    configInfo.put("cli/scode", scode);
    configInfo.put("cli/tor", Boolean.toString(tor));
    configInfo.putAll(torConfig.getConfigInfo());
    if (dojo != null) {
      configInfo.putAll(dojo.getConfigInfo());
    } else {
      configInfo.put("cli/dojo", "null");
    }
    configInfo.put("cli/apiKey", ClientUtils.maskString(apiKey));
    configInfo.put("cli/seedEncrypted", ClientUtils.maskString(seed));
    configInfo.put("cli/persistDelay", Integer.toString(persistDelay));
    configInfo.put("cli/refreshPoolsDelay", Integer.toString(refreshPoolsDelay));
    configInfo.put("cli/tx0MinConfirmations", Integer.toString(tx0MinConfirmations));
    configInfo.put("cli/proxy", proxy != null ? ClientUtils.maskString(proxy) : "null");
    configInfo.putAll(mix.getConfigInfo());
    configInfo.putAll(api.getConfigInfo());
    return configInfo;
  }
}
