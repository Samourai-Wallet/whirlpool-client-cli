package com.samourai.whirlpool.cli.config;

import com.samourai.whirlpool.cli.services.JavaHttpClientService;
import com.samourai.whirlpool.cli.services.JavaStompClientService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
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

  public boolean isTor() {
    return tor;
  }

  public void setTor(boolean tor) {
    this.tor = tor;
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
    String xpub = server.getFeeXpub();
    String xpubMasked =
        xpub.substring(0, 6) + "..." + xpub.substring(xpub.length() - 4, xpub.length());
    configInfo.put(
        "server",
        "url="
            + server.getServerUrl()
            + ", network="
            + server.getParams()
            + ", ssl="
            + Boolean.toString(server.isSsl())
            + ", feeXpub="
            + xpubMasked
            + ", feeValue="
            + server.getFeeValue());
    configInfo.put("pushtx", pushtx);
    configInfo.put("tor", Boolean.toString(tor));
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

  public WhirlpoolWalletConfig computeWhirlpoolWalletConfig() {
    WhirlpoolWalletConfig config = new WhirlpoolWalletConfig(httpClient, stompClient, server);
    if (!Strings.isEmpty(scode)) {
      config.setScode(scode);
    }

    config.setMaxClients(mix.getClients());
    config.setClientDelay(mix.getClientDelay());
    config.setTx0Delay(mix.getTx0Delay());
    config.setAutoTx0(mix.isAutoTx0());
    config.setAutoMix(mix.isAutoMix());

    if (mix.getPoolIdsByPriority() != null && !mix.getPoolIdsByPriority().isEmpty()) {
      config.setPoolIdsByPriority(mix.getPoolIdsByPriority());
    }
    return config;
  }
}
