package com.samourai.tor.client;

import com.msopentech.thali.toronionproxy.OsData;
import com.msopentech.thali.toronionproxy.TorSettings;
import com.samourai.http.client.HttpUsage;
import com.samourai.tor.client.utils.WhirlpoolTorInstaller;
import com.samourai.whirlpool.cli.beans.CliProxy;
import com.samourai.whirlpool.cli.beans.CliTorExecutableMode;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.exception.NotifiableException;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaTorClient {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private CliConfig cliConfig;
  private Collection<HttpUsage> torHttpUsages;
  private TorOnionProxyInstance torInstance;
  private boolean started = false;

  public JavaTorClient(CliConfig cliConfig, Collection<HttpUsage> torHttpUsages) {
    this.cliConfig = cliConfig;
    this.torHttpUsages = torHttpUsages;
  }

  public void setup() throws Exception {
    TorSettings torSettings = computeTorSettings();
    WhirlpoolTorInstaller torInstaller = computeTorInstaller();
    this.torInstance = new TorOnionProxyInstance(torInstaller, torSettings, torHttpUsages);
  }

  private WhirlpoolTorInstaller computeTorInstaller() throws Exception {
    CliTorExecutableMode executableMode = cliConfig.getTorConfig().getExecutableMode();
    String executablePath =
        CliTorExecutableMode.SPECIFIED.equals(executableMode)
            ? cliConfig.getTorConfig().getExecutable()
            : null;
    boolean tryEmbedded = CliTorExecutableMode.AUTO.equals(executableMode);

    // try with embedded
    Optional<File> torExecutable =
        computeTorExecutable(executableMode, executablePath, tryEmbedded);
    try {
      // verify Tor executable is supported
      return checkTorInstaller(torExecutable); // throws exception when Tor not supported
    } catch (Exception e) {
      if (tryEmbedded) {
        log.warn(
            "Tor executable failed ("
                + (torExecutable.isPresent() ? torExecutable.get().getAbsolutePath() : "embedded")
                + ") => trying fallback...");
        try {
          // retry without embedded
          tryEmbedded = false;
          torExecutable = computeTorExecutable(executableMode, executablePath, tryEmbedded);
          try {
            return checkTorInstaller(torExecutable); // throws exception when Tor not supported
          } catch (Exception ee) {
            log.error(
                "Tor executable failed ("
                    + (torExecutable.isPresent()
                        ? torExecutable.get().getAbsolutePath()
                        : "embedded")
                    + ") => Tor is not available",
                ee);
            throw e;
          }
        } catch (Exception ee) {
          log.error("initial (embedded) error", e);
          throw ee;
        }
      } else {
        log.error(
            "Tor executable failed ("
                + (torExecutable.isPresent() ? torExecutable.get().getAbsolutePath() : "embedded")
                + ") => Tor is not available",
            e);
        throw e;
      }
    }
  }

  private Optional<File> computeTorExecutable(
      CliTorExecutableMode executableMode, String executablePath, boolean tryEmbedded)
      throws NotifiableException {
    // path specified
    if (CliTorExecutableMode.SPECIFIED.equals(executableMode)) {
      if (log.isDebugEnabled()) {
        log.debug("Using tor executable (specified): " + executablePath);
      }
      return Optional.of(getTorExecutablePath(executablePath));
    }

    // embedded
    if (tryEmbedded && !OsData.OsType.UNSUPPORTED.equals(OsData.getOsType())) {
      if (log.isDebugEnabled()) {
        log.debug("Using tor executable (embedded): " + OsData.getOsType());
      }
      return Optional.empty();
    }

    // find local Tor executable
    Optional<File> torExecutable = findTorExecutableLocal();
    if (!torExecutable.isPresent()) {
      // not found
      if (log.isDebugEnabled()) {
        log.debug("Using tor executable (local): no local install found");
      }
      throw new NotifiableException(
          "No local Tor executable found on your system, please install Tor.");
    }
    if (log.isDebugEnabled()) {
      log.debug("Using tor executable (local): " + torExecutable.get());
    }
    return torExecutable;
  }

  private Optional<File> findTorExecutableLocal() {
    try {
      // try uppercase
      List<String> whichResult = CliUtils.execOrEmpty("which Tor");
      if (whichResult.isEmpty()) {
        // try lowercase
        whichResult = CliUtils.execOrEmpty("which tor");
      }
      if (whichResult.size() > 0) {
        if (log.isDebugEnabled()) {
          log.debug("Tor executable found in: " + StringUtils.join(whichResult.toArray(), ";"));
        }
        String path = whichResult.get(0).trim();
        if (!path.isEmpty()) {
          File torExecutable = new File(path);
          if (torExecutable.exists()) {
            return Optional.of(torExecutable);
          } else {
            log.warn("Tor executable not found in " + path);
          }
        }
      }
    } catch (Exception e) {
      log.error("", e);
    }
    return Optional.empty();
  }

  private File getTorExecutablePath(String path) throws NotifiableException {
    File file = new File(path);
    if (!file.exists()) {
      throw new NotifiableException("cli.torConfig.executable is invalid: file not found: " + path);
    }
    if (!file.isFile() || !file.canExecute()) {
      throw new NotifiableException(
          "cli.torConfig.executable is invalid: file not executable: " + path);
    }
    if (log.isDebugEnabled()) {
      log.debug("cli.torConfig.executable is valid: " + path);
    }
    return file;
  }

  private WhirlpoolTorInstaller checkTorInstaller(Optional<File> torExecutable) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug(
          "Verifying Tor executable ("
              + (torExecutable.isPresent() ? torExecutable.get().getAbsolutePath() : "embedded")
              + ")");
    }
    int fileCreationTimeout = cliConfig.getTorConfig().getFileCreationTimeout();
    WhirlpoolTorInstaller torInstaller =
        new WhirlpoolTorInstaller("whirlpoolTor", torExecutable, fileCreationTimeout);
    torInstaller.setup(); // throws
    return torInstaller;
  }

  public void connect() {
    if (started) {
      log.warn("connect: already started");
      return;
    }
    if (log.isDebugEnabled()) {
      log.debug("Connecting Tor...");
    }

    torInstance.start();
    started = true;
  }

  public void waitReady() throws NotifiableException {
    if (!started) {
      connect();
    }
    torInstance.waitReady();
    log.info("Tor is ready");
  }

  public void changeIdentity() {
    if (!started) {
      if (log.isDebugEnabled()) {
        log.debug("Changing Tor identity -> connect");
      }
      connect();
    } else {
      if (log.isDebugEnabled()) {
        log.debug("Changing Tor identity");
      }

      torInstance.changeIdentity();
    }
  }

  public void shutdown() {
    started = false;
    torInstance.clear();
    torInstance = null;
  }

  public int getProgress() {
    if (torInstance == null) {
      return 0;
    }
    return torInstance.getProgress();
  }

  public Optional<CliProxy> getTorProxy(HttpUsage httpUsage) {
    return torInstance.getTorProxy(httpUsage);
  }

  private TorSettings computeTorSettings() throws Exception {
    String customTorrc = null;
    String customTorrcFilename = cliConfig.getTorConfig().getCustomTorrc();
    if (!StringUtils.isEmpty(customTorrcFilename)) {
      try {
        customTorrc = new String(Files.readAllBytes(Paths.get(customTorrcFilename)));
      } catch (Exception e) {
        throw new NotifiableException(
            "Cannot read cli.torConfig.customTorrc file: " + customTorrcFilename, e);
      }
    }
    TorSettings torSettings = new JavaTorSettings(cliConfig.getCliProxy(), customTorrc);
    return torSettings;
  }
}
