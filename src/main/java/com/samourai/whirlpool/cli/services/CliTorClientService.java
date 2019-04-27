package com.samourai.whirlpool.cli.services;

import com.samourai.tor.client.JavaTorClient;
import com.samourai.tor.client.JavaTorConnexion;
import com.samourai.whirlpool.cli.config.CliConfig;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
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

  public void connect() {
    Optional<JavaTorClient> torClient = getTorClient();
    if (torClient.isPresent()) {
      // Tor enabled
      torClient.get().connect();
    }
  }

  public void disconnect() {
    Optional<JavaTorClient> torClient = getTorClient();
    if (torClient.isPresent()) {
      // Tor enabled
      torClient.get().disconnect();
    }
  }

  private Optional<JavaTorClient> getTorClient() {
    if (cliConfig.getTor()) {
      if (!torClient.isPresent()) {
        if (log.isDebugEnabled()) {
          log.debug("Enabling TOR.");
        }
        // connect (1 shared connexion for all TOR traffic)
        torClient = Optional.of(new JavaTorClient(0));
        torClient.get().connect();
      }
    } else {
      if (torClient.isPresent()) {
        if (log.isDebugEnabled()) {
          log.debug("Disabling TOR.");
        }
        // disconnect
        torClient.get().disconnect();
        torClient = Optional.empty();
      }
    }
    return torClient;
  }

  public Optional<JavaTorConnexion> getTorConnexion(boolean isRegisterOutput) {
    if (useTor(isRegisterOutput)) {
      // REGISTER_OUTPUT
      Optional<JavaTorClient> torClient = getTorClient();
      if (torClient.isPresent()) {
        // Tor enabled
        return Optional.of(torClient.get().getConnexion(false));
      }
    }
    // TOR disabled
    return Optional.empty();
  }

  private boolean useTor(boolean isRegisterOutput) {
    return cliConfig.getTor();
  }
}
