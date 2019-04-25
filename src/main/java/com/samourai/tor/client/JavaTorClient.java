package com.samourai.tor.client;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import org.silvertunnel_ng.netlib.api.NetFactory;
import org.silvertunnel_ng.netlib.api.NetLayerIDs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaTorClient {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int SLEEP_WAIT_READY = 1000;

  private NetFactory sharedNetFactory;
  private List<NetFactory> netFactories = new ArrayList<>();
  private int nbPrivateConnexions;

  public JavaTorClient(int nbPrivateConnexions) {
    this.nbPrivateConnexions = nbPrivateConnexions;
  }

  public void connect() {
    // start connecting
    if (log.isDebugEnabled()) {
      log.debug("Connecting");
    }
    adjustConnexions();
    waitSharedConnexionReady();
    waitPrivateConnexionReady(this.nbPrivateConnexions);
  }

  public void disconnect() {
    if (log.isDebugEnabled()) {
      log.debug("Disconnecting");
    }
    if (sharedNetFactory != null) {
      sharedNetFactory.clearRegisteredNetLayers();
      sharedNetFactory = null;
    }

    netFactories.forEach(privateNetFactory -> privateNetFactory.clearRegisteredNetLayers());
    netFactories.clear();
  }

  public boolean isConnected() {
    return sharedNetFactory != null;
  }

  public JavaTorConnexion getConnexion(boolean privateCircuit) {
    NetFactory netFactory = getNetFactory(privateCircuit);
    return new JavaTorConnexion(netFactory);
  }

  private synchronized NetFactory getNetFactory(boolean privateCircuit) {
    if (!isConnected()) {
      connect();
    }
    NetFactory netFactory = privateCircuit ? getNetFactoryReady() : getSharedNetFactory();
    return netFactory;
  }

  private synchronized NetFactory getSharedNetFactory() {
    if (sharedNetFactory == null) {
      if (log.isDebugEnabled()) {
        log.debug("Creating sharedNetFactory");
      }
      sharedNetFactory = createNetFactory();
    }
    return sharedNetFactory;
  }

  public NetFactory waitSharedConnexionReady() {
    NetFactory sharedNetFactory = getSharedNetFactory();
    int lastReadyPercent = 0;
    while (!isReady(sharedNetFactory)) {
      int readyPercent = getReadyPercent(sharedNetFactory);
      if (readyPercent != lastReadyPercent) {
        log.info("Connecting TOR... (" + readyPercent + "%)");
      }
      lastReadyPercent = readyPercent;
    }
    return sharedNetFactory;
  }

  private synchronized NetFactory getNetFactoryReady() {
    // get first NetFactory ready
    NetFactory netFactory = null;
    double lastBestIndicator = -1;
    while (true) {
      double bestIndicator = 0;
      for (NetFactory nf : netFactories) {
        double indicator = nf.getNetLayerById(NetLayerIDs.TOR).getStatus().getReadyIndicator();
        if (indicator == 1.0) {
          netFactory = nf;
          break;
        }
        if (indicator > bestIndicator) {
          bestIndicator = indicator;
        }
      }
      if (netFactory != null) {
        break;
      }
      if (bestIndicator != lastBestIndicator) {
        log.info("Connecting TOR... (" + (Math.round(bestIndicator) * 100) + "%)");
        lastBestIndicator = bestIndicator;
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }

    // remove
    netFactories.remove(netFactory);
    adjustConnexions();

    return netFactory;
  }

  public void waitPrivateConnexionReady(int nbConnexions) {
    for (int i = 0; i < nbConnexions; i++) {
      double lastBestIndicator = -1;
      while (true) {
        int nbReady = 0;
        double bestIndicator = 0;
        for (NetFactory nf : netFactories) {
          if (isReady(nf)) {
            nbReady++;
            if (nbReady == nbConnexions) {
              return;
            }
          } else {
            double indicator = getReadyPercent(nf);
            if (indicator > bestIndicator) {
              bestIndicator = indicator;
              lastBestIndicator = bestIndicator;
            }
          }
        }
        if (bestIndicator != lastBestIndicator) {
          log.info(
              "Connecting TOR " + nbReady + "/" + nbConnexions + "... (" + bestIndicator + "%)");
        }
        try {
          Thread.sleep(SLEEP_WAIT_READY);
        } catch (InterruptedException e) {
        }
      }
    }
  }

  private boolean isReady(NetFactory nf) {
    double indicator = nf.getNetLayerById(NetLayerIDs.TOR).getStatus().getReadyIndicator();
    return indicator == 1.0;
  }

  private int getReadyPercent(NetFactory nf) {
    double indicator = nf.getNetLayerById(NetLayerIDs.TOR).getStatus().getReadyIndicator();
    return (int) Math.round(indicator) * 100;
  }

  private void adjustConnexions() {
    int nbToAdd = nbPrivateConnexions - netFactories.size();
    if (nbToAdd == 0) {
      return;
    }

    if (nbToAdd > 0) {
      // create missing connexions
      for (int i = 0; i < nbToAdd; i++) {
        if (log.isDebugEnabled()) {
          log.debug("New private connexion: " + (i + 1) + "/" + nbToAdd);
        }
        netFactories.add(createNetFactory());
      }
    } else {
      // remove exceeding connexions
      int nbToClose = -nbToAdd;
      for (int i = 0; i < nbToClose; i++) {
        if (log.isDebugEnabled()) {
          log.debug("Closing private connexion: " + (i + 1) + "/" + nbToClose);
        }
        getNetFactory(true).clearRegisteredNetLayers();
      }
    }
  }

  private NetFactory createNetFactory() {
    NetFactory netFactory = new NetFactory();
    netFactory.getNetLayerById(NetLayerIDs.TOR); // start connecting
    return netFactory;
  }

  protected synchronized void removeConnexion(NetFactory netFactory) {
    this.netFactories.remove(netFactory);
    adjustConnexions();
  }

  public void setNbPrivateConnexions(int nbPrivateConnexions) {
    this.nbPrivateConnexions = nbPrivateConnexions;
    adjustConnexions();
  }
}
