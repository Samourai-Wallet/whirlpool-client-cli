package com.samourai.whirlpool.cli.config;

import com.samourai.http.client.IHttpClient;
import com.samourai.stomp.client.IStompClient;
import com.samourai.whirlpool.cli.beans.CliProxy;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import com.samourai.whirlpool.client.wallet.persist.WhirlpoolWalletPersistHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.NotEmpty;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "cli")
@Configuration
public abstract class CliConfigFile {
  private WhirlpoolServer server;
  private String scode;
  @NotEmpty private String pushtx;
  @NotEmpty private boolean tor;
  @NotEmpty private String apiKey;
  @NotEmpty private String seed;
  @NotEmpty private int persistDelay;
  @NotEmpty private String proxy;
  private Optional<CliProxy> _cliProxy;
  @NotEmpty private MixConfig mix;

  private static final String PUSHTX_AUTO = "auto";
  private static final String PUSHTX_INTERACTIVE = "interactive";

  public CliConfigFile() {
    // warning: properties are NOT loaded yet
    // it will be loaded later on SpringBoot application run()
  }

  public CliConfigFile(CliConfigFile copy) {
    this.server = copy.server;
    this.scode = copy.scode;
    this.pushtx = copy.pushtx;
    this.tor = copy.tor;
    this.apiKey = copy.apiKey;
    this.seed = copy.seed;
    this.persistDelay = copy.persistDelay;
    this.proxy = copy.proxy;
    this.mix = new MixConfig(copy.mix);
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

  public boolean isPushtxInteractive() {
    return PUSHTX_INTERACTIVE.equals(pushtx);
  }

  public boolean isPushtxAuto() {
    return PUSHTX_AUTO.equals(pushtx);
  }

  public boolean isPushtxCli() {
    return !PUSHTX_INTERACTIVE.equals(pushtx) && !PUSHTX_AUTO.equals(pushtx);
  }

  public String getPushtx() {
    return pushtx;
  }

  public void setPushtx(String pushtx) {
    this.pushtx = pushtx;
  }

  public boolean getTor() {
    return tor;
  }

  public void setTor(boolean tor) {
    this.tor = tor;
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

  public int getPersistDelay() {
    return persistDelay;
  }

  public void setPersistDelay(int persistDelay) {
    this.persistDelay = persistDelay;
  }

  public String getProxy() {
    return proxy;
  }

  public Optional<CliProxy> getCliProxy() {
    if (_cliProxy == null) {
      _cliProxy = Optional.ofNullable(CliUtils.computeProxyOrNull(proxy));
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

  public static class MixConfig {
    @NotEmpty private int clients;
    @NotEmpty private int clientDelay;
    @NotEmpty private int tx0Delay;
    @NotEmpty private int tx0MaxOutputs;
    @NotEmpty private boolean autoMix;
    @NotEmpty private int mixsTarget;

    public MixConfig() {}

    public MixConfig(MixConfig copy) {
      this.clients = copy.clients;
      this.clientDelay = copy.clientDelay;
      this.tx0Delay = copy.tx0Delay;
      this.tx0MaxOutputs = copy.tx0MaxOutputs;
      this.autoMix = copy.autoMix;
      this.mixsTarget = copy.mixsTarget;
    }

    public int getClients() {
      return clients;
    }

    public void setClients(int clients) {
      this.clients = clients;
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
      configInfo.put("cli/mix/clients", Integer.toString(clients));
      configInfo.put("cli/mix/clientDelay", Integer.toString(clientDelay));
      configInfo.put("cli/mix/tx0Delay", Integer.toString(tx0Delay));
      configInfo.put("cli/mix/tx0MaxOutputs", Integer.toString(tx0MaxOutputs));
      configInfo.put("cli/mix/autoMix", Boolean.toString(autoMix));
      configInfo.put("cli/mix/mixsTarget", Integer.toString(mixsTarget));
      return configInfo;
    }
  }

  public WhirlpoolWalletConfig computeWhirlpoolWalletConfig(
      IHttpClient httpClient,
      IStompClient stompClient,
      WhirlpoolWalletPersistHandler persistHandler)
      throws NotifiableException {
    String serverUrl = computeServerUrl();
    WhirlpoolWalletConfig config =
        new WhirlpoolWalletConfig(httpClient, stompClient, persistHandler, serverUrl, server);
    if (!Strings.isEmpty(scode)) {
      config.setScode(scode);
    }
    config.setPersistDelay(persistDelay);

    config.setMaxClients(mix.getClients());
    config.setClientDelay(mix.getClientDelay());
    config.setTx0Delay(mix.getTx0Delay());
    config.setTx0MaxOutputs(mix.getTx0MaxOutputs() > 0 ? mix.getTx0MaxOutputs() : null);
    config.setAutoMix(mix.isAutoMix());
    config.setMixsTarget(mix.getMixsTarget());

    return config;
  }

  private String computeServerUrl() {
    // better clearnet over torV2 than flawed onionV2
    // String serverUrl = tor ? server.getServerOnionV2() : server.getServerUrl();
    String serverUrl = server.getServerUrl();
    return serverUrl;
  }

  public Map<String, String> getConfigInfo() {
    Map<String, String> configInfo = new HashMap<>();
    configInfo.put("cli/server", server.name());
    configInfo.put("cli/scode", scode);
    configInfo.put("cli/pushtx", ClientUtils.maskString(pushtx, 3, 3));
    configInfo.put("cli/tor", Boolean.toString(tor));
    configInfo.put("cli/apiKey", ClientUtils.maskString(apiKey, 3, 3));
    configInfo.put("cli/seedEncrypted", ClientUtils.maskString(seed, 3, 3));
    configInfo.put("cli/persistDelay", Integer.toString(persistDelay));
    configInfo.put("cli/proxy", proxy != null ? ClientUtils.maskString(proxy, 3, 3) : "null");
    configInfo.putAll(mix.getConfigInfo());
    return configInfo;
  }
}
