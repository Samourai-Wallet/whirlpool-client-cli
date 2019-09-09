package com.samourai.whirlpool.cli.api.protocol.beans;

import com.samourai.whirlpool.cli.beans.CliProxy;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.config.CliConfigFile;
import com.samourai.whirlpool.cli.services.CliConfigService;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import java.lang.invoke.MethodHandles;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiCliConfig {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private String server;
  private String scode;
  private Boolean tor;
  private Boolean dojo;
  private String proxy;
  private ApiMixConfig mix;

  public static final String KEY_SERVER = "cli.server";
  private static final String KEY_SCODE = "cli.scode";
  public static final String KEY_TOR = "cli.tor";
  private static final String KEY_PROXY = "cli.proxy";
  private static final String KEY_MIX_CLIENTS_PER_POOL = "cli.mix.clientsPerPool";
  private static final String KEY_MIX_CLIENT_DELAY = "cli.mix.clientDelay";
  private static final String KEY_MIX_TX0_MAX_OUTPUTS = "cli.mix.tx0MaxOutputs";
  private static final String KEY_MIX_AUTO_MIX = "cli.mix.autoMix";
  private static final String KEY_MIX_MIXS_TARGET = "cli.mix.mixsTarget";

  public ApiCliConfig() {}

  public ApiCliConfig(CliConfig cliConfig) {
    this.server = cliConfig.getServer().name();
    this.scode = cliConfig.getScode();
    this.tor = cliConfig.getTor();
    this.dojo = cliConfig.getDojo().isEnabled();
    this.proxy = cliConfig.getProxy();
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
      props.put(KEY_TOR, Boolean.toString(tor));
    }

    if (dojo != null) {
      props.put(CliConfigService.KEY_DOJO_ENABLED, Boolean.toString(dojo));
    }

    if (proxy != null) {
      if (!StringUtils.isEmpty(proxy) && !CliProxy.validate(proxy)) {
        throw new NotifiableException("Invalid value for: proxy");
      }
      props.put(KEY_PROXY, proxy.trim());
    }

    if (mix != null) {
      mix.toProperties(props);
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

  public Boolean getTor() {
    return tor;
  }

  public void setTor(Boolean tor) {
    this.tor = tor;
  }

  public Boolean getDojo() {
    return dojo;
  }

  public void setDojo(Boolean dojo) {
    this.dojo = dojo;
  }

  public String getProxy() {
    return proxy;
  }

  public void setProxy(String proxy) {
    this.proxy = proxy;
  }

  public ApiMixConfig getMix() {
    return mix;
  }

  public void setMix(ApiMixConfig mix) {
    this.mix = mix;
  }

  public static class ApiMixConfig {
    private Integer clientsPerPool;
    private Integer clientDelay;
    private Integer tx0MaxOutputs;
    private Boolean autoMix;
    private Integer mixsTarget;

    public ApiMixConfig() {}

    public ApiMixConfig(CliConfigFile.MixConfig mixConfig) {
      this.clientsPerPool = mixConfig.getClientsPerPool();
      this.clientDelay = mixConfig.getClientDelay();
      this.tx0MaxOutputs = mixConfig.getTx0MaxOutputs();
      this.autoMix = mixConfig.isAutoMix();
      this.mixsTarget = mixConfig.getMixsTarget();
    }

    public void toProperties(Properties props) throws NotifiableException {
      if (clientsPerPool != null) {
        if (clientsPerPool < 1) {
          throw new NotifiableException("mix.clientsPerPool should be > 0");
        }
        props.put(KEY_MIX_CLIENTS_PER_POOL, Integer.toString(clientsPerPool));
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
      if (autoMix != null) {
        props.put(KEY_MIX_AUTO_MIX, Boolean.toString(autoMix));
      }
      if (mixsTarget != null) {
        if (mixsTarget < 1) {
          throw new NotifiableException("mix.mixTargets should be > 0");
        }
        props.put(KEY_MIX_MIXS_TARGET, Integer.toString(mixsTarget));
      }
    }

    public Integer getClientsPerPool() {
      return clientsPerPool;
    }

    public void setClientsPerPool(Integer clientsPerPool) {
      this.clientsPerPool = clientsPerPool;
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

    public Boolean getAutoMix() {
      return autoMix;
    }

    public void setAutoMix(Boolean autoMix) {
      this.autoMix = autoMix;
    }

    public Integer getMixsTarget() {
      return mixsTarget;
    }

    public void setMixsTarget(Integer mixsTarget) {
      this.mixsTarget = mixsTarget;
    }
  }
}
