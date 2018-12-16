package com.samourai.whirlpool.client.run;

import com.samourai.api.SamouraiApi;
import com.samourai.rpc.client.RpcClientService;
import com.samourai.whirlpool.client.utils.Bip84ApiWallet;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunUpgradeCli {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int CLI_VERSION_TX0_PAYLOAD = 1;

  private NetworkParameters params;
  private SamouraiApi samouraiApi;
  private Optional<RpcClientService> rpcClientService;
  private Bip84ApiWallet depositAndPremixWallet;

  public RunUpgradeCli(
      NetworkParameters params,
      SamouraiApi samouraiApi,
      Optional<RpcClientService> rpcClientService,
      Bip84ApiWallet depositAndPremixWallet) {
    this.params = params;
    this.samouraiApi = samouraiApi;
    this.rpcClientService = rpcClientService;
    this.depositAndPremixWallet = depositAndPremixWallet;
  }

  public void run(int lastVersion) throws Exception {
    // run upgrades
    if (lastVersion < CLI_VERSION_TX0_PAYLOAD) {
      upgradeTx0Payload();
    }
  }

  private void upgradeTx0Payload() throws Exception {
    // consolidate premix to force new tx0
    log.info(" • Upgrade: consolidating premix wallet for new tx0 payload...");
    new RunAggregateWallet(params, samouraiApi, rpcClientService, depositAndPremixWallet)
        .run(depositAndPremixWallet);
  }
}
