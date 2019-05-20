package com.samourai.whirlpool.cli.services;

import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.wallet.pushTx.PushTxService;

// NOT autowired: wrapped with CliPushTxService
public class InteractivePushTxService implements PushTxService {

  @Override
  public boolean testConnectivity() {
    return System.console() != null;
  }

  @Override
  public void pushTx(String txHex) throws Exception {
    String message =
        "Please broadcast manually the following transaction (or restart with --rpc-client-url=http://user:password@yourBtcNode:port):\n"
            + txHex
            + "\n";
    CliUtils.waitUserAction(message);
  }
}
