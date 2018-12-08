package com.samourai.tor.client;

import java.net.URL;
import java.net.URLStreamHandler;
import org.silvertunnel_ng.netlib.adapter.url.NetlibURLStreamHandlerFactory;
import org.silvertunnel_ng.netlib.api.NetFactory;
import org.silvertunnel_ng.netlib.api.NetLayer;
import org.silvertunnel_ng.netlib.api.NetLayerIDs;

public class JavaTorConnexion {
  private NetFactory netFactory;

  public JavaTorConnexion(NetFactory netFactory) {
    this.netFactory = netFactory;
  }

  public URL getUrl(String urlStr) throws Exception {
    NetlibURLStreamHandlerFactory streamHandlerFactory = computeStreamHandlerFactory(netFactory);
    String protocol = urlStr.split("://")[0];
    URLStreamHandler handler = streamHandlerFactory.createURLStreamHandler(protocol);
    URL url = new URL(null, urlStr, handler);
    return url;
  }

  private NetlibURLStreamHandlerFactory computeStreamHandlerFactory(NetFactory netFactory) {
    NetLayer netLayer = netFactory.getNetLayerById(NetLayerIDs.TOR);
    netLayer.waitUntilReady(); // wait connected

    NetlibURLStreamHandlerFactory urlFactory = new NetlibURLStreamHandlerFactory(false);
    urlFactory.setNetLayerForHttpHttpsFtp(netLayer);
    return urlFactory;
  }

  public void close() {
    netFactory.clearRegisteredNetLayers();
  }
}
