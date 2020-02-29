package com.samourai.tor.client;

import com.msopentech.thali.toronionproxy.OsData;
import com.msopentech.thali.toronionproxy.TorSettings;
import com.samourai.tor.client.utils.WhirlpoolTorInstaller;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.exception.NotifiableException;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaTorClient {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String TOR_DIR_SHARED = "whirlpoolTorShared";
  private static final String TOR_DIR_REG_OUT = "whirlpoolTorRegOut";

  private CliConfig cliConfig;
  private TorOnionProxyInstance torInstanceShared;
  private TorOnionProxyInstance torInstanceRegOut;
  private boolean started = false;

  public JavaTorClient(CliConfig cliConfig) {
    this.cliConfig = cliConfig;
  }

  public void setup() throws Exception {
    TorSettings torSettings = computeTorSettings();
    Optional<File> torExecutable = computeTorExecutableAndVerify();

    // setup Tor instances
    this.torInstanceShared =
        new TorOnionProxyInstance(
            new WhirlpoolTorInstaller(TOR_DIR_SHARED, torExecutable), torSettings, "shared");

    // run second instance on different ports
    this.torInstanceRegOut =
        new TorOnionProxyInstance(
            new WhirlpoolTorInstaller(TOR_DIR_REG_OUT, torExecutable), torSettings, "regOut");
  }

  private Optional<File> computeTorExecutableAndVerify() throws Exception {
    boolean torExecutableAuto = cliConfig.getTorConfig().isExecutableAuto();
    boolean torExecutableLocal = cliConfig.getTorConfig().isExecutableLocal();
    String executablePath = cliConfig.getTorConfig().getExecutable();

    // try with embedded
    boolean torExecutableEmbedded = true;
    Optional<File> torExecutable =
        computeTorExecutable(
            torExecutableAuto, torExecutableEmbedded, torExecutableLocal, executablePath);
    try {
      // verify Tor executable is supported
      checkTorExecutable(torExecutable); // throws exception when Tor not supported
    } catch (Exception e) {
      if (torExecutableAuto && torExecutableEmbedded) {
        log.warn(
            "Tor executable failed ("
                + (torExecutable.isPresent() ? torExecutable.get().getAbsolutePath() : "embedded")
                + ") => trying fallback...");
        // retry without embedded
        torExecutableEmbedded = false;
        torExecutable =
            computeTorExecutable(
                torExecutableAuto, torExecutableEmbedded, torExecutableLocal, executablePath);
        try {
          checkTorExecutable(torExecutable); // throws exception when Tor not supported
        } catch (Exception ee) {
          log.error(
              "Tor executable failed ("
                  + (torExecutable.isPresent() ? torExecutable.get().getAbsolutePath() : "embedded")
                  + ") => Tor is not available",
              ee);
          throw e;
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
    return torExecutable;
  }

  private Optional<File> computeTorExecutable(
      boolean torExecutableAuto,
      boolean torExecutableEmbedded,
      boolean torExecutableLocal,
      String executablePath)
      throws NotifiableException {
    if (!torExecutableAuto && !torExecutableLocal) {
      // use specified path for Tor executable
      if (log.isDebugEnabled()) {
        log.debug("Using tor executable: " + executablePath);
      }
      return Optional.of(getTorExecutablePath(executablePath));
    }

    // auto => embedded supported?
    if (torExecutableAuto
        && torExecutableEmbedded
        && !OsData.OsType.UNSUPPORTED.equals(OsData.getOsType())) {
      // use embedded Tor
      if (log.isDebugEnabled()) {
        log.debug("Using tor executable: embedded");
      }
      return Optional.empty();
    }

    // auto + embedded not supported => search for local Tor executable
    if (log.isDebugEnabled()) {
      log.debug("Using tor executable: OS not supported, looking for existing local install");
    }
    Optional<File> torExecutable = findTorExecutableLocal();

    // no Tor executable found
    if (!torExecutable.isPresent()) {
      throw new NotifiableException(
          "No local Tor executable found on your system, please install Tor.");
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

  private void checkTorExecutable(Optional<File> torExecutable) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug(
          "Verifying Tor executable ("
              + (torExecutable.isPresent() ? torExecutable.get().getAbsolutePath() : "embedded")
              + ")");
    }
    new WhirlpoolTorInstaller("temp", torExecutable).setup(); // throws
  }

  public void connect() {
    if (started) {
      log.warn("connect: already started");
      return;
    }
    if (log.isDebugEnabled()) {
      log.debug("Connecting");
    }

    torInstanceShared.start();
    torInstanceRegOut.start();
    started = true;
  }

  public void waitReady() throws NotifiableException {
    if (!started) {
      connect();
    }
    torInstanceShared.waitReady();
    torInstanceRegOut.waitReady();
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

      torInstanceShared.changeIdentity();
      torInstanceRegOut.changeIdentity();
    }
  }

  public void disconnect() {
    if (log.isDebugEnabled()) {
      log.debug("Disconnecting");
    }

    started = false;
    torInstanceShared.stop();
    torInstanceRegOut.stop();
  }

  public void shutdown() {
    started = false;
    torInstanceShared.clear();
    torInstanceRegOut.clear();
    torInstanceShared = null;
    torInstanceRegOut = null;
  }

  public JavaTorConnexion getConnexion(boolean isRegisterOutput) {
    TorOnionProxyInstance torInstance = isRegisterOutput ? torInstanceRegOut : torInstanceShared;
    return torInstance;
  }

  private TorSettings computeTorSettings() {
    TorSettings torSettings = new JavaTorSettings(cliConfig.getCliProxy());
    return torSettings;
  }
}
