package com.samourai.whirlpool.cli.beans;

import java8.util.Optional;

public enum TorMode {
  REGISTER_OUTPUT,
  ALL,
  FALSE;

  private TorMode() {}

  public static Optional<TorMode> find(String value) {
    if (value != null && "TRUE".equals(value.toUpperCase())) {
      // default TOR mode
      return Optional.of(REGISTER_OUTPUT);
    }
    try {
      return Optional.of(valueOf(value));
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
