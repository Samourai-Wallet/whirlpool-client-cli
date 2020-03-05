package com.samourai.whirlpool.cli.beans;

import java.util.Optional;

public enum CliTorExecutableMode {
  AUTO, // try embedded then find local
  LOCAL, // find local install
  SPECIFIED; // custom path specified

  public static Optional<CliTorExecutableMode> find(String value) {
    try {
      return Optional.of(valueOf(value));
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
