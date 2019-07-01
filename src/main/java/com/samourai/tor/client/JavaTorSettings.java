package com.samourai.tor.client;

import com.msopentech.thali.toronionproxy.DefaultSettings;
import com.samourai.whirlpool.cli.beans.CliProxy;
import com.samourai.whirlpool.cli.beans.CliProxyProtocol;
import java.util.Optional;

public class JavaTorSettings extends DefaultSettings {
  private CliProxy cliProxy;
  private int portOffset;

  public JavaTorSettings(Optional<CliProxy> cliProxy, int portOffset) {
    this.cliProxy = cliProxy.orElse(null);
    this.portOffset = portOffset;
  }

  @Override
  public String dnsPort() {
    return Integer.toString(5400 + portOffset);
  }

  @Override
  public int getHttpTunnelPort() {
    return 8118 + portOffset;
  }

  @Override
  public int getRelayPort() {
    return 9001 + portOffset;
  }

  @Override
  public String getSocksPort() {
    return Integer.toString(9050 + portOffset);
  }

  @Override
  public String transPort() {
    // not available on mac
    return "0";
  }

  @Override
  public boolean runAsDaemon() {
    return false;
  }

  @Override
  public boolean hasSafeSocks() {
    // prevent DNS leaks
    return true;
  }

  @Override
  public String getProxyHost() {
    if (cliProxy != null && CliProxyProtocol.HTTP.equals(cliProxy.getProtocol())) {
      return cliProxy.getHost();
    }
    return null;
  }

  @Override
  public String getProxyPort() {
    if (cliProxy != null && CliProxyProtocol.HTTP.equals(cliProxy.getProtocol())) {
      return Integer.toString(cliProxy.getPort());
    }
    return null;
  }

  @Override
  public String getProxySocks5Host() {
    if (cliProxy != null && CliProxyProtocol.SOCKS.equals(cliProxy.getProtocol())) {
      return cliProxy.getHost();
    }
    return null;
  }

  @Override
  public String getProxySocks5ServerPort() {
    if (cliProxy != null && CliProxyProtocol.SOCKS.equals(cliProxy.getProtocol())) {
      return Integer.toString(cliProxy.getPort());
    }
    return null;
  }

  @Override
  public String toString() {
    return "TorSettings[proxy="
        + (cliProxy != null ? cliProxy : "null")
        + ", portOffset="
        + portOffset;
  }
}
