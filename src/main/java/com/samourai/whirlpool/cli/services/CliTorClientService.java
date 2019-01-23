package com.samourai.whirlpool.cli.services;

import com.samourai.tor.client.JavaTorClient;
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

  public Optional<JavaTorClient> getTorClient() {
    if (!cliConfig.isTor() && torClient.isPresent()) {
      if (log.isDebugEnabled()) {
        log.debug("TOR config changed: false");
      }
      torClient.get().disconnect();
      torClient = Optional.empty();
    }
    if (cliConfig.isTor() && !torClient.isPresent()) {
      if (log.isDebugEnabled()) {
        log.debug("TOR config changed: true");
      }
      torClient = Optional.of(new JavaTorClient());
      torClient.get().connect();
    }
    return torClient;
  }
}
