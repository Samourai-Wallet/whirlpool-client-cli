package com.samourai.whirlpool.cli.services;

import com.samourai.tor.client.JavaTorClient;
import com.samourai.tor.client.JavaTorConnexion;
import com.samourai.whirlpool.cli.config.CliConfig;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import org.silvertunnel_ng.netlib.adapter.java.JvmGlobalUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// JavaTorClient wrapper for watching for cliConfig changes
@Service
public class CliTorClientService {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Optional<JavaTorClient> torClient;
  private CliConfig cliConfig;

  public CliTorClientService(CliConfig cliConfig) {
    this.torClient = Optional.empty();
    this.cliConfig = cliConfig;
  }

  public void init() {
    if (cliConfig.getTor()) {
      if (log.isDebugEnabled()) {
        log.debug("Initializing TOR");
      }
      // init Silvertunnel
      JvmGlobalUtil.init();
    }
  }

  public void connect() {
    Optional<JavaTorClient> torClient = getTorClient();
    if (torClient.isPresent()) {
      torClient.get().connect();
    }
  }

  public void waitReady() {
    Optional<JavaTorClient> torClient = getTorClient();
    if (torClient.isPresent()) {
      torClient.get().waitReady();
    }
  }

  public void disconnect() {
    Optional<JavaTorClient> torClient = getTorClient();
    if (torClient.isPresent()) {
      torClient.get().disconnect();
    }
  }

  public void changeCircuit() {
    Optional<JavaTorClient> torClient = getTorClient();
    if (torClient.isPresent()) {
      torClient.get().changeCircuit();
    }
  }

  public Optional<JavaTorConnexion> getTorConnexion(boolean isRegisterOutput) {
    if (cliConfig.getTor()) {
      Optional<JavaTorClient> torClient = getTorClient();
      if (torClient.isPresent()) {
        // Tor enabled
        JavaTorConnexion torConnexion = torClient.get().getConnexion(isRegisterOutput);
        return Optional.of(torConnexion);
      }
    }
    // TOR disabled
    return Optional.empty();
  }

  private Optional<JavaTorClient> getTorClient() {
    if (cliConfig.getTor()) {
      if (!torClient.isPresent()) {
        if (log.isDebugEnabled()) {
          log.debug("Enabling TOR.");
        }
        // connect
        torClient = Optional.of(new JavaTorClient());
        connect();
      }
    } else {
      if (torClient.isPresent()) {
        if (log.isDebugEnabled()) {
          log.debug("Disabling TOR.");
        }
        // disconnect
        disconnect();
        torClient = Optional.empty();
      }
    }
    return torClient;
  }
}
