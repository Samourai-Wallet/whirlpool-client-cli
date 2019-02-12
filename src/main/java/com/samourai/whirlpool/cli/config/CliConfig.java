package com.samourai.whirlpool.cli.config;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.validation.constraints.NotEmpty;
import org.apache.logging.log4j.util.Strings;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "cli")
@Configuration
public class CliConfig {
  @NotEmpty private boolean testnet;
  @NotEmpty private NetworkParameters networkParameters;
  @NotEmpty private ServerConfig server;
  private String scode;
  @NotEmpty private String pushtx;
  @NotEmpty private boolean tor;
  @NotEmpty private FeeConfig fee;
  @NotEmpty private MixConfig mix;

  private static final String PUSHTX_AUTO = "auto";
  private static final String PUSHTX_INTERACTIVE = "interactive";

  public boolean isTestnet() {
    return testnet;
  }

  public void setNetwork(String network) {
    this.testnet = TestNet3Params.get().getPaymentProtocolId().equals(network);
    NetworkParameters networkParameters = testnet ? TestNet3Params.get() : MainNetParams.get();
    this.networkParameters = networkParameters;
  }

  public NetworkParameters getNetworkParameters() {
    return networkParameters;
  }

  public ServerConfig getServer() {
    return server;
  }

  public void setServer(ServerConfig server) {
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

  public boolean isTor() {
    return tor;
  }

  public void setTor(boolean tor) {
    this.tor = tor;
  }

  public FeeConfig getFee() {
    return fee;
  }

  public void setFee(FeeConfig fee) {
    this.fee = fee;
  }

  public MixConfig getMix() {
    return mix;
  }

  public void setMix(MixConfig mix) {
    this.mix = mix;
  }

  public static class ServerConfig {
    @NotEmpty private String url;
    @NotEmpty private boolean ssl;

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public boolean isSsl() {
      return ssl;
    }

    public void setSsl(boolean ssl) {
      this.ssl = ssl;
    }
  }

  public static class FeeConfig {
    @NotEmpty private String xpub;
    @NotEmpty private long value;

    public String getXpub() {
      return xpub;
    }

    public void setXpub(String xpub) {
      this.xpub = xpub;
    }

    public long getValue() {
      return value;
    }

    public void setValue(long value) {
      this.value = value;
    }
  }

  public static class MixConfig {
    @NotEmpty private int clients;
    @NotEmpty private int clientDelay;
    @NotEmpty private int tx0Delay;
    @NotEmpty private boolean autoTx0;
    @NotEmpty private boolean autoMix;
    @NotEmpty private boolean autoAggregatePostmix;
    @NotEmpty private Collection<String> poolIdsByPriority;

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
  }

  public Map<String, String> getConfigInfo() {
    Map<String, String> configInfo = new LinkedHashMap<>();
    configInfo.put(
        "server",
        "url="
            + server.getUrl()
            + ", network="
            + networkParameters.getId()
            + ", testnet="
            + testnet);
    configInfo.put("pushtx", pushtx);
    configInfo.put("tor", Boolean.toString(tor));
    configInfo.put(
        "fee",
        "xpub="
            + fee.xpub.substring(0, 6)
            + "..."
            + fee.xpub.substring(fee.xpub.length() - 4, fee.xpub.length()));
    String poolIdsByPriorityStr = "null";
    if (mix.getPoolIdsByPriority() != null && !mix.getPoolIdsByPriority().isEmpty()) {
      poolIdsByPriorityStr = Strings.join(mix.getPoolIdsByPriority(), ',');
    }
    configInfo.put(
        "mix",
        "clients="
            + mix.getClients()
            + ", clientDelay="
            + mix.getClientDelay()
            + ", tx0Delay="
            + mix.getTx0Delay()
            + ", autoTx0="
            + mix.isAutoTx0()
            + ", autoMix="
            + mix.isAutoMix()
            + ", autoAggregatePostmix="
            + mix.isAutoAggregatePostmix()
            + ", poolIdsByPriority="
            + poolIdsByPriorityStr);
    return configInfo;
  }
}
