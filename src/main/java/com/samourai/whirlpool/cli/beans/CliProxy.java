package com.samourai.whirlpool.cli.beans;

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

  @Override
  public String toString() {
    return protocol + "://" + host + ":" + port;
  }
}
