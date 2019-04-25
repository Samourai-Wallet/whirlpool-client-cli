package com.samourai.whirlpool.cli.beans;

import java.util.Optional;

public enum CliProxyProtocol {
  HTTP,
  SOCKS;

  public static Optional<CliProxyProtocol> find(String value) {
    try {
      return Optional.of(valueOf(value));
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
