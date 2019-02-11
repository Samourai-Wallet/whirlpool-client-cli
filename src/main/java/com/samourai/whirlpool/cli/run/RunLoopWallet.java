package com.samourai.whirlpool.cli.run;

import com.samourai.api.client.SamouraiApi;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.services.WalletAggregateService;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.MixOrchestratorState;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolWalletState;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunLoopWallet {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int SLEEP_LOOPWALLET = 5000;
  private static final int SLEEP_LOOPWALLET_ON_ERROR = 30000;

  private CliConfig cliConfig;
  private SamouraiApi samouraiApi;
  private CliWalletService cliWalletService;
  private WalletAggregateService walletAggregateService;
  private Tx0Service tx0Service;

  public RunLoopWallet(
      CliConfig cliConfig,
      SamouraiApi samouraiApi,
      CliWalletService cliWalletService,
      WalletAggregateService walletAggregateService,
      Tx0Service tx0Service) {
    this.cliConfig = cliConfig;
    this.samouraiApi = samouraiApi;
    this.cliWalletService = cliWalletService;
    this.walletAggregateService = walletAggregateService;
    this.tx0Service = tx0Service;
  }

  public void run() throws Exception {
    while (true) {
      // wait for wallet startup... then wait for clientDelay before checking again...
      Thread.sleep((cliConfig.getMix().getClientDelay() * 1000) + SLEEP_LOOPWALLET);
      try {
        runLoopIteration();
      } catch (Exception e) {
        if (e instanceof NotifiableException) {
          log.error(e.getMessage());
        } else {
          log.error("", e);
        }
        Thread.sleep(SLEEP_LOOPWALLET_ON_ERROR);
      }
    }
  }

  private void runLoopIteration() throws Exception {
    WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();
    WhirlpoolWalletState whirlpoolWalletState = whirlpoolWallet.getState();
    MixOrchestratorState mixState = whirlpoolWalletState.getMixState();
    log.info(
        "[Whirlpool state] "
            + mixState.getNbMixing()
            + "/"
            + mixState.getMaxClients()
            + " threads, "
            + mixState.getNbIdle()
            + " idle, "
            + mixState.getNbQueued()
            + " to mix");

    int iThread = 1;
    for (WhirlpoolUtxo whirlpoolUtxo : mixState.getUtxosMixing()) {
      log.info("[Thread " + iThread + "] " + whirlpoolUtxo.toString());
      iThread++;
    }
  }
}
