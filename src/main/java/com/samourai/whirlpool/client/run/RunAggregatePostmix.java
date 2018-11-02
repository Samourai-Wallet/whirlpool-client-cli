package com.samourai.whirlpool.client.run;

import com.samourai.api.beans.UnspentResponse;
import com.samourai.rpc.client.RpcClientService;
import com.samourai.whirlpool.client.CliUtils;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.Tx0;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunAggregatePostmix {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private NetworkParameters params;
  private Optional<RpcClientService> rpcClientService;

  public RunAggregatePostmix(
      NetworkParameters params, Optional<RpcClientService> rpcClientService) {
    this.params = params;
    this.rpcClientService = rpcClientService;
  }

  public Tx0 run(VpubWallet vpubWallet) throws Exception {
    List<UnspentResponse.UnspentOutput> utxos = vpubWallet.fetchUtxos(RunVPubLoop.ACCOUNT_POSTMIX);
    if (!utxos.isEmpty()) {
      log.info("Found " + utxos.size() + " utxo from postmix:");
      CliUtils.printUtxos(utxos);
    } else {
      throw new NotifiableException("No utxo found from postmix.");
    }
    return null;
  }
}
