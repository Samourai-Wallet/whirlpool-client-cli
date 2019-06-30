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

  public void connect() {
    Optional<JavaTorClient> torClient = getTorClient();
    if (torClient.isPresent()) {
      torClient.get().connect();
    }
  }

  public void waitReady() throws NotifiableException {
    Optional<JavaTorClient> torClient = getTorClient();
    if (torClient.isPresent()) {
      torClient.get().waitReady();
    }
  }

  private void disconnect() {
    Optional<JavaTorClient> torClient = getTorClient();
    if (torClient.isPresent()) {
      torClient.get().disconnect();
    }
  }

  public void shutdown() {
    Optional<JavaTorClient> torClient = getTorClient();
    if (torClient.isPresent()) {
      torClient.get().shutdown();
    }
  }

  public void changeIdentity() {
    Optional<JavaTorClient> torClient = getTorClient();
    if (torClient.isPresent()) {
      torClient.get().changeIdentity();
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

  public Optional<Integer> getProgress() {
    Optional<JavaTorClient> torClient = getTorClient();
    if (!torClient.isPresent()) {
      return Optional.empty();
    }

    // average progress of the two connexions
    int progressShared = torClient.get().getConnexion(false).getProgress();
    int progressRegOut = torClient.get().getConnexion(true).getProgress();
    int progress = (progressShared + progressRegOut) / 2;
    return Optional.of(progress);
  }

  private Optional<JavaTorClient> getTorClient() {
    if (cliConfig.getTor()) {
      if (!torClient.isPresent()) {
        if (log.isDebugEnabled()) {
          log.debug("Enabling TOR.");
        }
        // instanciate TorClient
        try {
          torClient = Optional.of(new JavaTorClient(cliConfig));
        } catch (Exception e) {
          log.error("", e);
          torClient = Optional.empty();
        }
      }
    } else {
      if (torClient.isPresent()) {
        if (log.isDebugEnabled()) {
          log.debug("Disabling TOR.");
        }
        // disconnect and clear TorClient
        disconnect();
        torClient = Optional.empty();
      }
    }
    return torClient;
  }
}
