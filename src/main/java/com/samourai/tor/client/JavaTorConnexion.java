package com.samourai.tor.client;

import java.net.URL;
import java.net.URLStreamHandler;
import org.silvertunnel_ng.netlib.adapter.url.NetlibURLStreamHandlerFactory;
import org.silvertunnel_ng.netlib.api.NetFactory;
import org.silvertunnel_ng.netlib.api.NetLayer;
import org.silvertunnel_ng.netlib.api.NetLayerIDs;

public class JavaTorConnexion {
  private NetFactory netFactory;
  private boolean privateCircuit;
  private NetlibURLStreamHandlerFactory urlFactory;

  public JavaTorConnexion(NetFactory netFactory, boolean privateCircuit) {
    this.netFactory = netFactory;
    this.privateCircuit = privateCircuit;
  }

  public URL getUrl(String urlStr) throws Exception {
    NetlibURLStreamHandlerFactory streamHandlerFactory = computeStreamHandlerFactory(netFactory);
    String protocol = urlStr.split("://")[0];
    URLStreamHandler handler = streamHandlerFactory.createURLStreamHandler(protocol);
    URL url = new URL(null, urlStr, handler);
    return url;
  }

  private NetlibURLStreamHandlerFactory computeStreamHandlerFactory(NetFactory netFactory) {
    if (urlFactory == null) {
      NetLayer netLayer = netFactory.getNetLayerById(NetLayerIDs.TOR);
      netLayer.waitUntilReady(); // wait connected

      urlFactory = new NetlibURLStreamHandlerFactory(false);
      urlFactory.setNetLayerForHttpHttpsFtp(netLayer);
    }
    return urlFactory;
  }

  public void close() {
    close(false);
  }

  public void close(boolean closeShared) {
    if (privateCircuit || closeShared) {
      netFactory.clearRegisteredNetLayers();
    }
  }
}
