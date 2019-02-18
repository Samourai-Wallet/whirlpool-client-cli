package com.samourai.whirlpool.cli.services;

import com.samourai.rpc.client.JSONRpcClientServiceImpl;
import com.samourai.rpc.client.RpcClientService;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.client.wallet.pushTx.AbstractPushTxService;
import com.samourai.whirlpool.client.wallet.pushTx.PushTxService;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// PushTxService wrapper for watching for cliConfig changes
@Service
public class CliPushTxService extends AbstractPushTxService {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private CliConfig cliConfig;
  private SamouraiApiService samouraiApiService;
  private PushTxService pushTxService;

  public CliPushTxService(CliConfig cliConfig, SamouraiApiService samouraiApiService) {
    this.cliConfig = cliConfig;
    this.samouraiApiService = samouraiApiService;
    this.pushTxService = new InteractivePushTxService();
  }

  private PushTxService get() throws Exception {
    if (cliConfig.isPushtxInteractive() && !(pushTxService instanceof InteractivePushTxService)) {
      if (log.isDebugEnabled()) {
        log.debug("pushtx config changed: interactive");
      }
      pushTxService = new InteractivePushTxService();
    }
    if (cliConfig.isPushtxCli() && !(pushTxService instanceof RpcClientService)) {
      if (log.isDebugEnabled()) {
        log.debug("pushtx config changed: rpc");
      }
      String rpcClientUrl = cliConfig.getPushtx();
      pushTxService = new JSONRpcClientServiceImpl(rpcClientUrl, cliConfig.getServer().getParams());
    }
    if (cliConfig.isPushtxAuto() && !(pushTxService instanceof SamouraiApiService)) {
      if (log.isDebugEnabled()) {
        log.debug("pushtx config changed: auto");
      }
      pushTxService = samouraiApiService;
    }
    return pushTxService;
  }

  @Override
  public void pushTx(String txHex) throws Exception {
    get().pushTx(txHex);
  }

  @Override
  public boolean testConnectivity() {
    try {
      return get().testConnectivity();
    } catch (Exception e) {
      log.error("", e);
      return false;
    }
  }
}
