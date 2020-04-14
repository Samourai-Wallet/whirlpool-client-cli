package com.samourai.whirlpool.cli.services;

import com.samourai.tor.client.JavaTorClient;
import com.samourai.tor.client.JavaTorConnexion;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.client.exception.NotifiableException;
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

  public void setup() throws Exception {
    if (cliConfig.getTor()) {
      if (!torClient.isPresent()) {
        if (log.isDebugEnabled()) {
          log.debug("Enabling Tor.");
        }
        // instanciate & initialize
        JavaTorClient tc = new JavaTorClient(cliConfig);
        tc.setup(); // throws
        torClient = Optional.of(tc);
        if (log.isDebugEnabled()) {
          log.debug("Tor is enabled.");
        }
      }
    } else {
      if (log.isDebugEnabled()) {
        log.debug("Tor is disabled.");
      }
    }
  }

  public void connect() {
    if (torClient.isPresent()) {
      torClient.get().connect();
    }
  }

  public void waitReady() throws NotifiableException {
    if (torClient.isPresent()) {
      torClient.get().waitReady();
    }
  }

  private void disconnect() {
    if (torClient.isPresent()) {
      torClient.get().disconnect();
    }
  }

  public void shutdown() {
    if (torClient.isPresent()) {
      torClient.get().shutdown();
    }
  }

  public void changeIdentity() {
    if (torClient.isPresent()) {
      torClient.get().changeIdentity();
    }
  }

  public Optional<JavaTorConnexion> getTorConnexion() {
    if (cliConfig.getTor()) {
      if (torClient.isPresent()) {
        // Tor enabled
        JavaTorConnexion torConnexion = torClient.get().getConnexion();
        return Optional.of(torConnexion);
      }
    }
    // Tor disabled
    return Optional.empty();
  }

  public Optional<Integer> getProgress() {
    if (!torClient.isPresent()) {
      if (cliConfig.getTor()) {
        return Optional.of(0); // Tor is initializing
      }
      return Optional.empty(); // Tor is disabled
    }

    int progress = torClient.get().getProgress();
    return Optional.of(progress);
  }
}
