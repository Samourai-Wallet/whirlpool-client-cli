package com.samourai.whirlpool.cli.api.protocol.beans;

import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.config.CliConfig.MixConfig;
import java.util.Collection;

public class ApiCliConfig {
  private String server;
  private ApiMixConfig mix;

  public ApiCliConfig() {}

  public ApiCliConfig(CliConfig cliConfig) {
    this.server = cliConfig.getServer().name();
    this.mix = new ApiMixConfig(cliConfig.getMix());
  }

  public String getServer() {
    return server;
  }

  public void setServer(String server) {
    this.server = server;
  }

  public ApiMixConfig getMix() {
    return mix;
  }

  public void setMix(ApiMixConfig mix) {
    this.mix = mix;
  }

  public static class ApiMixConfig {
    private Boolean autoTx0;
    private Boolean autoMix;
    private Boolean autoAggregatePostmix;
    private Collection<String> poolIdsByPriority;

    public ApiMixConfig() {}

    public ApiMixConfig(MixConfig mixConfig) {
      this.autoTx0 = mixConfig.isAutoTx0();
      this.autoMix = mixConfig.isAutoMix();
      this.autoAggregatePostmix = mixConfig.isAutoAggregatePostmix();
    }

    public Boolean isAutoTx0() {
      return autoTx0;
    }

    public void setAutoTx0(Boolean autoTx0) {
      this.autoTx0 = autoTx0;
    }

    public Boolean isAutoMix() {
      return autoMix;
    }

    public void setAutoMix(Boolean autoMix) {
      this.autoMix = autoMix;
    }

    public Boolean isAutoAggregatePostmix() {
      return autoAggregatePostmix;
    }

    public void setAutoAggregatePostmix(Boolean autoAggregatePostmix) {
      this.autoAggregatePostmix = autoAggregatePostmix;
    }

    public Collection<String> getPoolIdsByPriority() {
      return poolIdsByPriority;
    }

    public void setPoolIdsByPriority(Collection<String> poolIdsByPriority) {
      this.poolIdsByPriority = poolIdsByPriority;
    }
  }
}
