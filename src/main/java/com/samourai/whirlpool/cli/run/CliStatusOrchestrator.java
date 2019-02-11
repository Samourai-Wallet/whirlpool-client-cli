package com.samourai.whirlpool.cli.run;

import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.exception.NoSessionWalletException;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.MixOrchestratorState;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolWalletState;
import com.samourai.whirlpool.client.wallet.orchestrator.AbstractOrchestrator;
import com.samourai.whirlpool.client.wallet.orchestrator.AutoTx0Orchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliStatusOrchestrator extends AbstractOrchestrator {
  private static final Logger log = LoggerFactory.getLogger(AutoTx0Orchestrator.class);

  private CliWalletService cliWalletService;
  private CliConfig cliConfig;

  public CliStatusOrchestrator(
      int loopDelay, CliWalletService cliWalletService, CliConfig cliConfig) {
    super(loopDelay);
    this.cliWalletService = cliWalletService;
    this.cliConfig = cliConfig;
  }

  @Override
  protected void runOrchestrator() {
    // log CLI status
    try {
      WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();
      log.info(
          "[CLI] SESSION WALLET is OPENED and "
              + (whirlpoolWallet.isStarted() ? "STARTED" : "STOPPED")
              + ", autoTx0="
              + cliConfig.getMix().isAutoTx0()
              + ", autoAggregatePostmix="
              + cliConfig.getMix().isAutoAggregatePostmix());
      WhirlpoolWalletState whirlpoolWalletState = whirlpoolWallet.getState();
      MixOrchestratorState mixState = whirlpoolWalletState.getMixState();
      log.info(
          "[CLI] "
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
        log.info("[CLI STATE][Thread " + iThread + "] " + whirlpoolUtxo.toString());
        iThread++;
      }
    } catch (NoSessionWalletException e) {
      log.info("[CLI STATE] NO SESSION WALLET OPENED.");
    }
  }
}
