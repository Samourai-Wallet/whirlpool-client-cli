package com.samourai.tor.client;

import com.msopentech.thali.java.toronionproxy.JavaOnionProxyContext;
import com.msopentech.thali.toronionproxy.*;
import com.samourai.tor.client.utils.WhirlpoolTorInstaller;
import com.samourai.whirlpool.cli.Application;
import com.samourai.whirlpool.cli.beans.CliProxy;
import com.samourai.whirlpool.cli.beans.CliProxyProtocol;
import com.samourai.whirlpool.client.exception.NotifiableException;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SocketUtils;

public class TorOnionProxyInstance implements JavaTorConnexion {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int PROGRESS_CONNECTING = 50;

  private OnionProxyManager onionProxyManager;
  private Thread startThread;
  private boolean torSocksReady = false;
  private CliProxy torSocksShared = null;
  private CliProxy torSocksRegOut = null;
  private int progress;

  public TorOnionProxyInstance(WhirlpoolTorInstaller torInstaller, TorSettings torSettings)
      throws Exception {
    TorConfig torConfig = torInstaller.getConfig();
    if (log.isDebugEnabled()) {
      log.debug("new TorOnionProxyInstance: " + torConfig + " ; " + torSettings);
    }

    JavaOnionProxyContext context = new JavaOnionProxyContext(torConfig, torInstaller, torSettings);
    onionProxyManager = new OnionProxyManager(context);

    TorConfigBuilder builder = onionProxyManager.getContext().newConfigBuilder().updateTorConfig();

    int socksPortShared = SocketUtils.findAvailableTcpPort();
    builder.socksPort(Integer.toString(socksPortShared), null);
    torSocksShared = new CliProxy(CliProxyProtocol.SOCKS, "127.0.0.1", socksPortShared);

    int socksPortRegOut = SocketUtils.findAvailableTcpPort();
    builder.socksPort(Integer.toString(socksPortRegOut), null);
    torSocksRegOut = new CliProxy(CliProxyProtocol.SOCKS, "127.0.0.1", socksPortRegOut);

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
            "TorOnionProxyInstance-start");
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
        log.debug("Tor connected! " + torSocksShared + " | " + torSocksRegOut);
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

    torSocksReady = false;
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
            "TorOnionProxyInstance-stop");
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
    boolean success = onionProxyManager.setNewIdentity();
    if (success) {
      // watch tor progress in a new thread
      Thread statusThread =
          new Thread(
              () -> {
                try {
                  waitReady(); // updates progress
                } catch (Exception e) {
                  log.error("", e);
                }
              },
              "TorOnionProxyInstance-status");
      statusThread.setDaemon(true);
      statusThread.start();
    } else {
      log.warn("changeIdentity failed, restarting Tor...");
      stop();
      start();
    }
  }

  protected int getProgress() {
    return progress;
  }

  @Override
  public CliProxy getTorProxy(boolean isRegisterOutput) throws NotifiableException {
    waitTorSocks();
    return isRegisterOutput ? torSocksRegOut : torSocksShared;
  }

  private void waitTorSocks() {
    while (!torSocksReady && onionProxyManager.isRunning()) {
      try {
        // we should have a connexion now
        onionProxyManager.getIPv4LocalHostSocksPort();
        torSocksReady = true;
        log.info("TorSocks started.");
      } catch (Exception e) {
        log.error("TorSocks error", e);
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
      }
    }
  }
}
