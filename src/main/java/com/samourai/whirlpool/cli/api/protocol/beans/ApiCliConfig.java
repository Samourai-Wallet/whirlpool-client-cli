package com.samourai.whirlpool.cli.api.protocol.beans;

import com.samourai.whirlpool.cli.beans.TorMode;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.config.CliConfig.MixConfig;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

public class ApiCliConfig {
  private String server;
  private String scode;
  private String tor;
  private ApiMixConfig mix;

  private static final String KEY_SERVER = "cli.server";
  private static final String KEY_SCODE = "cli.scode";
  private static final String KEY_TOR = "cli.tor";
  private static final String KEY_MIX_CLIENTS = "cli.mix.clients";
  private static final String KEY_MIX_CLIENT_DELAY = "cli.mix.clientDelay";
  private static final String KEY_MIX_TX0_MAX_OUTPUTS = "cli.mix.tx0MaxOutputs";
  private static final String KEY_MIX_AUTO_TX0 = "cli.mix.autoTx0";
  private static final String KEY_MIX_AUTO_MIX = "cli.mix.autoMix";
  private static final String KEY_MIX_AUTO_AGGREGATE_POSTMIX = "cli.mix.autoAggregatePostmix";
  private static final String KEY_MIX_POOL_IDS_BY_PRIORITY = "cli.mix.poolIdsByPriority";
  private static final String KEY_MIX_MIXS_TARGET = "cli.mix.mixsTarget";

  public ApiCliConfig() {}

  public ApiCliConfig(CliConfig cliConfig) {
    this.server = cliConfig.getServer().name();
    this.scode = cliConfig.getScode();
    this.tor = cliConfig.getTor().name();
    this.mix = new ApiMixConfig(cliConfig.getMix());
  }

  public void toProperties(Properties props) throws NotifiableException {
    // server is mandatory
    WhirlpoolServer whirlpoolServer =
        WhirlpoolServer.find(server)
            .orElseThrow(() -> new NotifiableException("Invalid value for: server"));
    props.put(KEY_SERVER, whirlpoolServer.name());

    if (scode != null) {
      props.put(KEY_SCODE, scode.trim());
    }

    if (tor != null) {
      TorMode torMode =
          TorMode.find(tor).orElseThrow(() -> new NotifiableException("Invalid value for: tor"));
      props.put(KEY_TOR, torMode.name());
    }

    if (mix != null) {
      mix.toProperties(props, whirlpoolServer);
    }
  }

  public String getServer() {
    return server;
  }

  public void setServer(String server) {
    this.server = server;
  }

  public String getScode() {
    return scode;
  }

  public void setScode(String scode) {
    this.scode = scode;
  }

  public String getTor() {
    return tor;
  }

  public void setTor(String tor) {
    this.tor = tor;
  }

  public ApiMixConfig getMix() {
    return mix;
  }

  public void setMix(ApiMixConfig mix) {
    this.mix = mix;
  }

  public static class ApiMixConfig {
    private Integer clients;
    private Integer clientDelay;
    private Integer tx0MaxOutputs;
    private Boolean autoTx0;
    private Boolean autoMix;
    private Boolean autoAggregatePostmix;
    private Collection<String> poolIdsByPriority;
    private Integer mixsTarget;

    public ApiMixConfig() {}

    public ApiMixConfig(MixConfig mixConfig) {
      this.clients = mixConfig.getClients();
      this.clientDelay = mixConfig.getClientDelay();
      this.tx0MaxOutputs = mixConfig.getTx0MaxOutputs();
      this.autoTx0 = mixConfig.isAutoTx0();
      this.autoMix = mixConfig.isAutoMix();
      this.autoAggregatePostmix = mixConfig.isAutoAggregatePostmix();
      this.poolIdsByPriority = new ArrayList<>();
      this.poolIdsByPriority.addAll(mixConfig.getPoolIdsByPriority());
      this.mixsTarget = mixConfig.getMixsTarget();
    }

    public void toProperties(Properties props, WhirlpoolServer whirlpoolServer)
        throws NotifiableException {
      if (clients != null) {
        if (clients < 1) {
          throw new NotifiableException("mix.clients should be > 0");
        }
        props.put(KEY_MIX_CLIENTS, Integer.toString(clients));
      }
      if (clientDelay != null) {
        if (clientDelay < 1) {
          throw new NotifiableException("mix.clientDelay should be > 1");
        }
        props.put(KEY_MIX_CLIENT_DELAY, Integer.toString(clientDelay));
      }
      if (tx0MaxOutputs != null) {
        if (tx0MaxOutputs < 0) {
          throw new NotifiableException("mix.tx0MaxOutputs should be >= 0");
        }
        props.put(KEY_MIX_TX0_MAX_OUTPUTS, Integer.toString(tx0MaxOutputs));
      }
      if (autoTx0 != null) {
        props.put(KEY_MIX_AUTO_TX0, Boolean.toString(autoTx0));
      }
      if (autoMix != null) {
        props.put(KEY_MIX_AUTO_MIX, Boolean.toString(autoMix));
      }
      if (autoAggregatePostmix != null) {
        if (autoAggregatePostmix) {
          if (WhirlpoolServer.MAIN.equals(whirlpoolServer)) {
            throw new NotifiableException("AutoAggregatePostmix cannot be enabled for MainNet");
          }
          if (!getAutoTx0()) {
            throw new NotifiableException("AutoAggregatePostmix cannot be enabled without AutoTx0");
          }
        }
        props.put(KEY_MIX_AUTO_AGGREGATE_POSTMIX, Boolean.toString(autoAggregatePostmix));
      }
      if (poolIdsByPriority != null) {
        // poolIdsByPriority[0] = '' => no pool preference
        String poolIds = String.join(",", poolIdsByPriority);
        props.put(KEY_MIX_POOL_IDS_BY_PRIORITY, poolIds);
      }
      if (mixsTarget != null) {
        if (mixsTarget < 1) {
          throw new NotifiableException("mix.mixTargets should be > 0");
        }
        props.put(KEY_MIX_MIXS_TARGET, Integer.toString(mixsTarget));
      }
    }

    public Integer getClients() {
      return clients;
    }

    public void setClients(Integer clients) {
      this.clients = clients;
    }

    public Integer getClientDelay() {
      return clientDelay;
    }

    public void setClientDelay(Integer clientDelay) {
      this.clientDelay = clientDelay;
    }

    public Integer getTx0MaxOutputs() {
      return tx0MaxOutputs;
    }

    public void setTx0MaxOutputs(Integer tx0MaxOutputs) {
      this.tx0MaxOutputs = tx0MaxOutputs;
    }

    public Boolean getAutoTx0() {
      return autoTx0;
    }

    public void setAutoTx0(Boolean autoTx0) {
      this.autoTx0 = autoTx0;
    }

    public Boolean getAutoMix() {
      return autoMix;
    }

    public void setAutoMix(Boolean autoMix) {
      this.autoMix = autoMix;
    }

    public Boolean getAutoAggregatePostmix() {
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

    public Integer getMixsTarget() {
      return mixsTarget;
    }

    public void setMixsTarget(Integer mixsTarget) {
      this.mixsTarget = mixsTarget;
    }
  }
}
