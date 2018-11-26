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

  private NetFactory sharedNetFactory;
  private List<NetFactory> netFactories = new ArrayList<>();
  private int nbPrivateConnexions;

  public JavaTorClient() {
    this.nbPrivateConnexions = 1;
  }

  public void connect() {
    // start connecting
    adjustPrivateConnexions();
  }

  public void disconnect() {
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
      sharedNetFactory = getNetFactoryReady();
    }
    return sharedNetFactory;
  }

  private synchronized NetFactory getNetFactoryReady() {
    // get first NetFactory ready
    NetFactory netFactory = null;
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
      log.info("Connecting TOR... (" + (Math.round(bestIndicator) * 100) + "%)");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }

    // remove
    netFactories.remove(netFactory);

    // create a new one for next time
    netFactories.add(createNetFactory());
    return netFactory;
  }

  private void adjustPrivateConnexions() {
    int nbToAdd = nbPrivateConnexions - netFactories.size();
    if (nbToAdd == 0) {
      return;
    }

    if (nbToAdd > 0) {
      // create missing connexions
      for (int i = 0; i < nbToAdd; i++) {
        if (log.isDebugEnabled()) {
          log.debug("New private connexion: " + i + "/" + nbToAdd);
        }
        netFactories.add(createNetFactory());
      }
    } else {
      // remove exceeding connexions
      int nbToClose = -nbToAdd;
      for (int i = 0; i < nbToClose; i++) {
        if (log.isDebugEnabled()) {
          log.debug("Closing private connexion: " + i + "/" + nbToClose);
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
    adjustPrivateConnexions();
  }

  public void setNbPrivateConnexions(int nbPrivateConnexions) {
    this.nbPrivateConnexions = nbPrivateConnexions;
    adjustPrivateConnexions();
  }
}
