package com.samourai.tor.client;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.LinkedList;
import java.util.Queue;
import org.silvertunnel_ng.netlib.adapter.url.NetlibURLStreamHandlerFactory;
import org.silvertunnel_ng.netlib.api.NetFactory;
import org.silvertunnel_ng.netlib.api.NetLayer;
import org.silvertunnel_ng.netlib.api.NetLayerIDs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaTorClient {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private NetFactory sharedNetFactory;
  private Queue<NetFactory> privateNetFactories = new LinkedList<>(); // FIFO
  private int nbPrivateConnexions;

  public JavaTorClient() {
    this.nbPrivateConnexions = 0;
  }

  public void connect() {
    // start shared connexion
    sharedNetFactory = createNetFactory();

    // start private connexions
    adjustPrivateConnexions();
  }

  public void disconnect() {
    if (sharedNetFactory != null) {
      sharedNetFactory.clearRegisteredNetLayers();
      sharedNetFactory = null;
    }

    privateNetFactories.forEach(privateNetFactory -> privateNetFactory.clearRegisteredNetLayers());
    privateNetFactories.clear();
  }

  public boolean isConnected() {
    return sharedNetFactory != null;
  }

  public URL getUrl(String urlStr, boolean privateCircuit) throws Exception {
    NetFactory netFactory = getNetFactory(privateCircuit);

    NetlibURLStreamHandlerFactory streamHandlerFactory = computeStreamHandlerFactory(netFactory);
    String protocol = urlStr.split("://")[0];
    URLStreamHandler handler = streamHandlerFactory.createURLStreamHandler(protocol);
    URL url = new URL(null, urlStr, handler);
    return url;
  }

  /*public NetSocket getNetSocket(String host, int port) throws Exception {
    final TcpipNetAddress netAddress = new TcpipNetAddress(host, port);
    NetSocket netSocket = NetFactory.getInstance().getNetLayerById(NetLayerIDs.TOR_OVER_TLS_OVER_TCPIP)
        .createNetSocket(null, null, netAddress);
    return netSocket;
  }*/

  private synchronized NetFactory getNetFactory(boolean privateCircuit) {
    if (!isConnected()) {
      connect();
    }

    NetFactory netFactory;
    if (privateCircuit) {
      // get private circuit (FIFO)
      netFactory = privateNetFactories.remove();

      // create a new one for next time
      privateNetFactories.add(createNetFactory());
    } else {
      netFactory = sharedNetFactory;
    }
    return netFactory;
  }

  private void adjustPrivateConnexions() {
    int nbToAdd = nbPrivateConnexions - privateNetFactories.size();
    if (nbToAdd == 0) {
      return;
    }

    if (nbToAdd > 0) {
      // create missing connexions
      for (int i = 0; i < nbToAdd; i++) {
        if (log.isDebugEnabled()) {
          log.debug("New private connexion: " + i + "/" + nbToAdd);
        }
        privateNetFactories.add(createNetFactory());
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

  private NetlibURLStreamHandlerFactory computeStreamHandlerFactory(NetFactory netFactory) {
    NetLayer netLayer = netFactory.getNetLayerById(NetLayerIDs.TOR);
    netLayer.waitUntilReady(); // wait connected

    NetlibURLStreamHandlerFactory urlFactory = new NetlibURLStreamHandlerFactory(false);
    urlFactory.setNetLayerForHttpHttpsFtp(netLayer);
    return urlFactory;
  }

  public void setNbPrivateConnexions(int nbPrivateConnexions) {
    this.nbPrivateConnexions = nbPrivateConnexions;
    adjustPrivateConnexions();
  }
}
