package com.samourai.tor.client;

import com.msopentech.thali.java.toronionproxy.JavaOnionProxyContext;
import com.msopentech.thali.java.toronionproxy.JavaTorInstaller;
import com.msopentech.thali.toronionproxy.*;
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
  private CliProxy torProxy = null;
  private int progress;

  public TorOnionProxyInstance(TorConfig torConfig, TorSettings torSettings, String logPrefix)
      throws Exception {
    this.log = ClientUtils.prefixLogger(log, logPrefix);
    if (log.isDebugEnabled()) {
      log.debug("new TorOnionProxyInstance: " + torConfig + " ; " + torSettings);
    }
    // setup TOR
    TorInstaller torInstaller = new JavaTorInstaller(torConfig);

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
      log.debug("starting TOR");
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
              } catch (Exception e) {
                log.error("", e);
                stop();
              }
            });
    startThread.start();
  }

  public void waitReady() throws NotifiableException {
    while (!checkReady()) {
      if (startThread == null) {
        throw new NotifiableException("Tor connect failed");
      }
      try {
        Thread.sleep(90);
      } catch (InterruptedException e) {
      }
    }
  }

  private boolean checkReady() {
    boolean ready = onionProxyManager.isRunning();
    if (ready && progress != 100) {
      if (log.isDebugEnabled()) {
        log.debug("TOR connected!");
      }
      progress = 100;
    }
    return ready;
  }

  public synchronized void stop() {
    if (log.isDebugEnabled()) {
      log.debug("stopping TOR");
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
  }

  public synchronized void clear() {
    if (log.isDebugEnabled()) {
      log.debug("clearing TOR");
    }
    new Thread(
            () -> {
              stop();
            })
        .start();
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
      log.warn("changeIdentity failed, restarting TOR...");
      stop();
      start();
    }
  }

  @Override
  public int getProgress() {
    return progress;
  }

  @Override
  public CliProxy getTorProxy() throws NotifiableException {
    if (torProxy == null) {
      if (startThread == null) {
        log.error("getTorProxy() called when not started");
        return null;
      }
      waitReady();
      try {
        torProxy =
            new CliProxy(
                CliProxyProtocol.SOCKS, "127.0.0.1", onionProxyManager.getIPv4LocalHostSocksPort());
      } catch (Exception e) {
        log.error("Unable to get local tor proxy", e);
      }
    }
    return torProxy;
  }
}
