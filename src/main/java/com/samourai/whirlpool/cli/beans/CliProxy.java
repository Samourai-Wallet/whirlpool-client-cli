package com.samourai.whirlpool.cli.beans;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;

public class CliProxy {
  private CliProxyProtocol protocol;
  private String host;
  private int port;

  public CliProxy(CliProxyProtocol protocol, String host, int port) {
    this.protocol = protocol;
    this.host = host;
    this.port = port;
  }

  public CliProxyProtocol getProtocol() {
    return protocol;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public static boolean validate(String proxy) {
    // check protocol
    String[] protocols =
        Arrays.stream(CliProxyProtocol.values()).map(p -> p.name()).toArray(String[]::new);
    String regex = "^(" + StringUtils.join(protocols, "|").toLowerCase() + ")://(.+?):([0-9]+)";
    return proxy.trim().toLowerCase().matches(regex);
  }

  @Override
  public String toString() {
    return protocol + "://" + host + ":" + port;
  }
}
