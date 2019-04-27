package com.samourai.tor.client;

import java.lang.invoke.MethodHandles;
import org.silvertunnel_ng.netlib.adapter.java.JvmGlobalUtil;
import org.silvertunnel_ng.netlib.api.NetFactory;
import org.silvertunnel_ng.netlib.api.NetLayer;
import org.silvertunnel_ng.netlib.api.NetLayerIDs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaTorClient {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int SLEEP_WAIT_READY = 1000;

  private NetFactory sharedNetFactory;
  private NetFactory registerOutputNetFactory;

  public JavaTorClient() {}

  public void connect() {
    // start connecting
    if (log.isDebugEnabled()) {
      log.debug("Connecting");
    }
    changeCircuit();
  }

  public void waitReady() {
    if (log.isDebugEnabled()) {
      log.debug("waitReady");
    }
    waitReady(sharedNetFactory);
    waitReady(registerOutputNetFactory);
  }

  private void waitReady(NetFactory nf) {
    sharedNetFactory.getNetLayerById(NetLayerIDs.TOR).waitUntilReady();
  }

  public void changeCircuit() {
    if (log.isDebugEnabled()) {
      log.debug("Changing TOR circuit");
    }
    NetFactory oldSharedNetFactory = sharedNetFactory;
    NetFactory oldRegisterOutputNetFactory = registerOutputNetFactory;

    sharedNetFactory = createNetFactory();
    registerOutputNetFactory = createNetFactory();

    // bind all traffic to sharedNetFactory
    NetLayer netLayer = sharedNetFactory.getNetLayerById(NetLayerIDs.TOR);
    JvmGlobalUtil.setNetLayerAndNetAddressNameService(netLayer, false);

    // disconnect old factories
    disconnect(oldSharedNetFactory);
    disconnect(oldRegisterOutputNetFactory);
  }

  public void disconnect() {
    if (log.isDebugEnabled()) {
      log.debug("Disconnecting");
    }

    disconnect(sharedNetFactory);
    disconnect(registerOutputNetFactory);

    sharedNetFactory = null;
    registerOutputNetFactory = null;
  }

  private void disconnect(NetFactory nf) {
    if (nf != null) {
      nf.clearRegisteredNetLayers();
    }
  }

  public JavaTorConnexion getConnexion(boolean isRegisterOutput) {
    NetFactory netFactory = isRegisterOutput ? registerOutputNetFactory : sharedNetFactory;
    return new JavaTorConnexion(netFactory, isRegisterOutput);
  }

  private NetFactory createNetFactory() {
    return new NetFactory();
  }
}
