package com.samourai.whirlpool.cli.config;

import com.samourai.whirlpool.cli.beans.CliProxy;
import com.samourai.whirlpool.cli.services.JavaHttpClientService;
import com.samourai.whirlpool.cli.services.JavaStompClientService;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import java.util.Collection;
import java.util.Optional;
import javax.validation.constraints.NotEmpty;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "cli")
@Configuration
public class CliConfig {
  @Autowired JavaHttpClientService httpClient;
  @Autowired JavaStompClientService stompClient;
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
    private boolean autoTx0 = false;
    @NotEmpty private boolean autoMix;
    @NotEmpty private boolean autoAggregatePostmix;
    @NotEmpty private Collection<String> poolIdsByPriority;
    @NotEmpty private int mixsTarget;

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

    public boolean isAutoTx0() {
      return autoTx0;
    }

    public void setAutoTx0(boolean autoTx0) {
      this.autoTx0 = autoTx0;
    }

    public boolean isAutoMix() {
      return autoMix;
    }

    public void setAutoMix(boolean autoMix) {
      this.autoMix = autoMix;
    }

    public boolean isAutoAggregatePostmix() {
      return autoAggregatePostmix;
    }

    public void setAutoAggregatePostmix(boolean autoAggregatePostmix) {
      this.autoAggregatePostmix = autoAggregatePostmix;
    }

    public Collection<String> getPoolIdsByPriority() {
      return poolIdsByPriority;
    }

    public void setPoolIdsByPriority(Collection<String> poolIdsByPriority) {
      this.poolIdsByPriority = poolIdsByPriority;
    }

    public int getMixsTarget() {
      return mixsTarget;
    }

    public void setMixsTarget(int mixsTarget) {
      this.mixsTarget = mixsTarget;
    }
  }

  public static class FeeConfig {
    @NotEmpty private int min;
    @NotEmpty private int max;
    @NotEmpty private int fallback;

    public int getMin() {
      return min;
    }

    public void setMin(int min) {
      this.min = min;
    }

    public int getMax() {
      return max;
    }

    public void setMax(int max) {
      this.max = max;
    }

    public int getFallback() {
      return fallback;
    }

    public void setFallback(int fallback) {
      this.fallback = fallback;
    }
  }

  public WhirlpoolWalletConfig computeWhirlpoolWalletConfig() {
    String serverUrl = computeServerUrl();
    WhirlpoolWalletConfig config =
        new WhirlpoolWalletConfig(httpClient, stompClient, serverUrl, server);
    if (!Strings.isEmpty(scode)) {
      config.setScode(scode);
    }
    config.setPersistDelay(persistDelay);

    config.setMaxClients(mix.getClients());
    config.setClientDelay(mix.getClientDelay());
    config.setTx0Delay(mix.getTx0Delay());
    config.setTx0MaxOutputs(mix.getTx0MaxOutputs() > 0 ? mix.getTx0MaxOutputs() : null);
    config.setAutoTx0(mix.isAutoTx0());
    config.setAutoMix(mix.isAutoMix());

    if (mix.getPoolIdsByPriority() != null && !mix.getPoolIdsByPriority().isEmpty()) {
      config.setPoolIdsByPriority(mix.getPoolIdsByPriority());
    }
    config.setMixsTarget(mix.getMixsTarget());

    return config;
  }

  private String computeServerUrl() {
    // better clearnet over torV2 than flawed onionV2
    // String serverUrl = tor ? server.getServerOnionV2() : server.getServerUrl();
    String serverUrl = server.getServerUrl();
    return serverUrl;
  }

  public void checkValid() throws NotifiableException {
    if (mix.isAutoAggregatePostmix() && !mix.isAutoTx0()) {
      throw new NotifiableException("--auto-tx0 is required for --auto-aggregate-postmix");
    }
  }
}
