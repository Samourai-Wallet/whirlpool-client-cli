package com.samourai.tor.client;

import com.msopentech.thali.toronionproxy.TorConfig;
import com.msopentech.thali.toronionproxy.TorSettings;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.client.exception.NotifiableException;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
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

  private TorConfig computeTorConfig(String dirName) throws Exception {
    File dir = Files.createTempDirectory(dirName).toFile();
    dir.deleteOnExit();
    TorConfig torConfig = new TorConfig.Builder(dir, dir).homeDir(dir).build();
    return torConfig;
  }

  public JavaTorClient(CliConfig cliConfig) throws Exception {
    this.cliConfig = cliConfig;

    // setup TOR instances
    this.torInstanceShared =
        new TorOnionProxyInstance(
            computeTorConfig(TOR_DIR_SHARED), computeTorSettings(0), "shared");

    // run second instance on different ports
    this.torInstanceRegOut =
        new TorOnionProxyInstance(
            computeTorConfig(TOR_DIR_REG_OUT), computeTorSettings(1), "regOut");
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
}
