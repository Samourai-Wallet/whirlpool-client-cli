//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.silvertunnel_ng.netlib.adapter.url;

import java.net.Proxy;
import java.net.URL;
import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.silvertunnel_ng.netlib.adapter.url.impl.net.http.HttpHandler;
import org.silvertunnel_ng.netlib.api.NetFactory;
import org.silvertunnel_ng.netlib.api.NetLayer;
import org.silvertunnel_ng.netlib.api.NetLayerIDs;
import org.silvertunnel_ng.netlib.layer.tls.TLSNetLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class URLGlobalUtil {
  private static final Logger LOG = LoggerFactory.getLogger(URLGlobalUtil.class);
  private static NetlibURLStreamHandlerFactory netlibURLStreamHandlerFactory;

  public URLGlobalUtil() {}

  public static synchronized void initURLStreamHandlerFactory() {
    NetLayer tcpipNetLayer = NetFactory.getInstance().getNetLayerById(NetLayerIDs.NOP);
    NetLayer tlsNetLayer = NetFactory.getInstance().getNetLayerById(NetLayerIDs.NOP);
    NetlibURLStreamHandlerFactory factory =
        new NetlibURLStreamHandlerFactory(tcpipNetLayer, tlsNetLayer, false);
    initURLStreamHandlerFactory(factory);
  }

  public static synchronized void initURLStreamHandlerFactory(
      NetlibURLStreamHandlerFactory factory) {
    try {
      (new HttpHandler((NetLayer) null)).openConnection((URL) null, (Proxy) null);
    } catch (Exception var4) {
      LOG.debug("Can be ignored be ignored", var4);
    }

    if (netlibURLStreamHandlerFactory == null) {
      try {
        netlibURLStreamHandlerFactory = factory;
        // URL.setURLStreamHandlerFactory(factory); // EDIT Samourai
        TomcatURLStreamHandlerFactory.getInstance().addUserFactory(factory);
      } catch (Throwable var3) {
        String msg =
            "URL.setURLStreamHandlerFactory() was already called before, but not from UrlUtil, i.e. maybe the wrong factory is set";
        LOG.warn(
            "URL.setURLStreamHandlerFactory() was already called before, but not from UrlUtil, i.e. maybe the wrong factory is set",
            var3);
      }
    }
  }

  public static synchronized void setNetLayerUsedByURLStreamHandlerFactory(
      NetLayer tcpipNetLayer, NetLayer tlsNetLayer) throws IllegalStateException {
    if (netlibURLStreamHandlerFactory == null) {
      throw new IllegalStateException(
          "initURLStreamHandlerFactory() must be called first (but was not)");
    } else {
      netlibURLStreamHandlerFactory.setNetLayerForHttpHttpsFtp(tcpipNetLayer, tlsNetLayer);
    }
  }

  public static synchronized void setNetLayerUsedByURLStreamHandlerFactory(NetLayer tcpipNetLayer)
      throws IllegalStateException {
    setNetLayerUsedByURLStreamHandlerFactory(tcpipNetLayer, new TLSNetLayer(tcpipNetLayer));
  }
}
