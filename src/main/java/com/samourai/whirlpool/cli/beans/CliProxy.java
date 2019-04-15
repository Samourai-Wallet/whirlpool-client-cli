package com.samourai.whirlpool.cli.beans;

public class CliProxy {
  private String host;
  private int port;

  public CliProxy(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  @Override
  public String toString() {
    return host + ":" + port;
  }
}
