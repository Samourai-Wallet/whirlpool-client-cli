package com.samourai.tor.client;

import com.msopentech.thali.java.toronionproxy.JavaOnionProxyContext;
import com.msopentech.thali.toronionproxy.*;
import com.samourai.tor.client.utils.WhirlpoolTorInstaller;
import com.samourai.whirlpool.cli.Application;
import com.samourai.whirlpool.cli.beans.CliProxy;
import com.samourai.whirlpool.cli.beans.CliProxyProtocol;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TorOnionProxyInstance implements JavaTorConnexion {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int PROGRESS_CONNECTING = 50;

  private OnionProxyManager onionProxyManager;
  private Thread startThread;
  private CliProxy torSocks = null;
  private int progress;

  public TorOnionProxyInstance(
      WhirlpoolTorInstaller torInstaller, TorSettings torSettings, String logPrefix)
      throws Exception {
    this.log = ClientUtils.prefixLogger(log, logPrefix);
    TorConfig torConfig = torInstaller.getConfig();
    if (log.isDebugEnabled()) {
      log.debug("new TorOnionProxyInstance: " + torConfig + " ; " + torSettings);
    }

    JavaOnionProxyContext context = new JavaOnionProxyContext(torConfig, torInstaller, torSettings);
    onionProxyManager = new OnionProxyManager(context);

    TorConfigBuilder builder = onionProxyManager.getContext().newConfigBuilder().updateTorConfig();
    onionProxyManager.getContext().getInstaller().updateTorConfigCustom(builder.asString());
    onionProxyManager.setup();

    startThread = null;
    progress = 0;
  }

  public synchronized void start() {
    if (startThread != null) {
      log.warn("Tor is already started");
      return;
    }

    if (log.isDebugEnabled()) {
      log.debug("Starting Tor connexion...");
    }
    progress = PROGRESS_CONNECTING;

    startThread =
        new Thread(
            () -> {
              try {
                boolean ok = onionProxyManager.startWithRepeat(4 * 60, 5, false);
                if (!ok) {
                  log.error("Couldn't start tor");
                  throw new RuntimeException("Couldn't start tor");
                }
                waitReady();
              } catch (Exception e) {
                log.error("Tor failed to start", e);
                Application.exitError(1);
              }
            },
            "start-TorOnionProxyInstance");
    startThread.setDaemon(true);
    startThread.start();
  }

  public void waitReady() throws NotifiableException {
    if (progress != 100) {
      if (log.isDebugEnabled()) {
        log.debug("Waiting for Tor connexion...");
      }
    }
    while (!checkReady()) {
      if (startThread == null) {
        throw new NotifiableException("Tor connect failed");
      }
      if (log.isDebugEnabled()) {
        log.debug("Waiting for Tor circuit... ");
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
      }
    }
  }

  private boolean checkReady() {
    boolean ready = onionProxyManager.isRunning();
    if (ready && progress != 100) {
      if (log.isDebugEnabled()) {
        String torProxy = "error";
        try {
          torProxy = getTorProxy().toString();
        } catch (Exception e) {
          log.error("", e);
        }
        log.debug("Tor connected! " + torProxy);
      }
      progress = 100;
    }
    if (!ready && progress == 100) {
      if (log.isDebugEnabled()) {
        log.debug("Tor disconnected!");
      }
      progress = PROGRESS_CONNECTING;
    }
    return ready;
  }

  public synchronized void stop() {
    if (log.isDebugEnabled()) {
      log.debug("stopping Tor");
    }
    startThread = null;
    progress = 0;

    try {
      onionProxyManager.stop();
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error("", e);
      }
    }

    torSocks = null;
  }

  public synchronized void clear() {
    if (log.isDebugEnabled()) {
      log.debug("clearing Tor");
    }
    Thread stopThread =
        new Thread(
            () -> {
              stop();
            },
            "stop-torOnionProxyInstance");
    stopThread.setDaemon(true);
    stopThread.start();
    /*try {
      onionProxyManager.killTorProcess();
    } catch (Exception e) {
      log.error("", e);
    }*/
    onionProxyManager.getContext().getConfig().getInstallDir().delete();
  }

  public void changeIdentity() {
    progress = PROGRESS_CONNECTING;
    if (!onionProxyManager.setNewIdentity()) {
      log.warn("changeIdentity failed, restarting Tor...");
      stop();
      start();
    }
  }

  @Override
  public int getProgress() {
    checkReady(); // update progress
    return progress;
  }

  @Override
  public CliProxy getTorProxy() throws NotifiableException {
    CliProxy proxy;
    while ((proxy = getTorSocksOrNull()) == null) {
      waitReady();
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
      }
    }
    return proxy;
  }

  private CliProxy getTorSocksOrNull() {
    if (torSocks == null && onionProxyManager.isRunning()) {
      try {
        // we should have a connexion now
        int socksPort = onionProxyManager.getIPv4LocalHostSocksPort();
        torSocks = new CliProxy(CliProxyProtocol.SOCKS, "127.0.0.1", socksPort);
        log.info("TorSocks started: " + torSocks);
      } catch (Exception e) {
        log.error("Unable to get TorSocks", e);
      }
    }
    return torSocks;
  }
}
