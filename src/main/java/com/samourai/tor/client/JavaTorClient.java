package com.samourai.tor.client;

import com.msopentech.thali.toronionproxy.OsData;
import com.msopentech.thali.toronionproxy.TorConfig;
import com.msopentech.thali.toronionproxy.TorSettings;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.exception.NotifiableException;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
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

  private TorConfig computeTorConfig(String dirName, Optional<File> torExecutable)
      throws Exception {
    File dir = Files.createTempDirectory(dirName).toFile();
    dir.deleteOnExit();

    TorConfig.Builder torConfigBuilder = new TorConfig.Builder(dir, dir).homeDir(dir);

    if (torExecutable.isPresent()) {
      if (log.isDebugEnabled()) {
        log.debug("Using existing Tor executable: " + torExecutable.get().getAbsolutePath());
      }
      // use existing local TOR instead of embedded one
      torConfigBuilder.torExecutable(torExecutable.get());
    }

    TorConfig torConfig = torConfigBuilder.build();
    return torConfig;
  }

  private Optional<File> findTorExecutableLocal() {
    try {
      List<String> whichResult = CliUtils.exec("which tor");
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
      throw new NotifiableException("torConfig.executable is invalid: file not found: " + path);
    }
    if (!file.isFile() || !file.canExecute()) {
      throw new NotifiableException(
          "torConfig.executable is invalid: file not executable: " + path);
    }
    if (log.isDebugEnabled()) {
      log.debug("torConfig.executable is valid: " + path);
    }
    return file;
  }

  public JavaTorClient(CliConfig cliConfig) throws Exception {
    this.cliConfig = cliConfig;

    Optional<File> torExecutable = computeTorExecutable();
    boolean useExecutableFromZip = !torExecutable.isPresent();

    // setup TOR instances
    this.torInstanceShared =
        new TorOnionProxyInstance(
            computeTorConfig(TOR_DIR_SHARED, torExecutable),
            computeTorSettings(0),
            "shared",
            useExecutableFromZip);

    // run second instance on different ports
    this.torInstanceRegOut =
        new TorOnionProxyInstance(
            computeTorConfig(TOR_DIR_REG_OUT, torExecutable),
            computeTorSettings(1),
            "regOut",
            useExecutableFromZip);
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
    if (log.isDebugEnabled()) {
      log.debug("waitReady");
    }
    torInstanceShared.waitReady();
    torInstanceRegOut.waitReady();
    log.info(
        "TOR is ready: shared="
            + torInstanceShared.getTorProxy()
            + ", regOut="
            + torInstanceRegOut.getTorProxy());
  }

  public void changeIdentity() {
    if (!started) {
      if (log.isDebugEnabled()) {
        log.debug("Changing TOR identity -> connect");
      }
      connect();
    } else {
      if (log.isDebugEnabled()) {
        log.debug("Changing TOR identity");
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

  private TorSettings computeTorSettings(int portOffset) {
    TorSettings torSettings = new JavaTorSettings(cliConfig.getCliProxy(), portOffset);
    return torSettings;
  }

  private Optional<File> computeTorExecutable() throws NotifiableException {
    boolean osUnsupported = OsData.OsType.UNSUPPORTED.equals(OsData.getOsType());
    boolean torExecutableAuto = cliConfig.getTorConfig().isExecutableAuto();

    Optional<File> torExecutable;
    if (torExecutableAuto) {
      if (!osUnsupported) {
        // use embedded TOR
        if (log.isDebugEnabled()) {
          log.debug("Using tor executable: embedded");
        }
        torExecutable = Optional.empty();
      } else {
        // search for local TOR executable
        if (log.isDebugEnabled()) {
          log.debug("Using tor executable: OS not supported, looking for existing local install");
        }
        torExecutable = findTorExecutableLocal();
        if (!torExecutable.isPresent()) {
          throw new NotifiableException(
              "No local TOR executable found on your system, please install TOR.");
        }
      }
    } else {
      // use specified path for TOR executable
      String executablePath = cliConfig.getTorConfig().getExecutable();
      if (log.isDebugEnabled()) {
        log.debug("Using tor executable: " + executablePath);
      }
      torExecutable = Optional.of(getTorExecutablePath(executablePath));
    }
    return torExecutable;
  }
}
