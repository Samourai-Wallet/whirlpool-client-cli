package com.samourai.whirlpool.cli.run;

import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.client.utils.WhirlpoolUtxoListener;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunMixWallet {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private CliWalletService cliWalletService;

  public RunMixWallet(CliWalletService cliWalletService) {
    this.cliWalletService = cliWalletService;
  }

  public boolean runMix(
      List<WhirlpoolUtxo> whirlpoolUtxos, Pool pool, int nbClients, int clientDelay)
      throws Exception {

    WhirlpoolUtxoListener utxoListener = new WhirlpoolUtxoListener();

    log.info(" • Adding " + nbClients + " utxos to mix...");
    for (int i = 0; i < nbClients; i++) {
      // pick last mustMix
      WhirlpoolUtxo whirlpoolUtxo = whirlpoolUtxos.remove(whirlpoolUtxos.size() - 1);
      utxoListener.register(whirlpoolUtxo);

      cliWalletService.getSessionWallet().addToMix(whirlpoolUtxo, pool);

      if (clientDelay > 0) {
        Thread.sleep(clientDelay);
      }
    }

    // quit when all utxos are mixed
    boolean success = utxoListener.waitDone();
    return success;
  }
}
