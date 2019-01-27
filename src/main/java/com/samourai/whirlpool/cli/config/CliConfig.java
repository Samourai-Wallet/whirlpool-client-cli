package com.samourai.whirlpool.cli.config;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.validation.constraints.NotEmpty;
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
  @NotEmpty private boolean debug;
  @NotEmpty private FeeConfig fee;

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

  public boolean isDebug() {
    return debug;
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  public FeeConfig getFee() {
    return fee;
  }

  public void setFee(FeeConfig fee) {
    this.fee = fee;
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
    configInfo.put("debug", Boolean.toString(debug));
    return configInfo;
  }
}
