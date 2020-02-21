package com.samourai.whirlpool.cli.run;

import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.exception.NoSessionWalletException;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.MixingState;
import com.samourai.whirlpool.client.wallet.orchestrator.AbstractOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliStatusOrchestrator extends AbstractOrchestrator {
  private static final Logger log = LoggerFactory.getLogger(CliStatusOrchestrator.class);

  private CliStatusInteractiveOrchestrator statusInteractiveOrchestrator;
  private CliWalletService cliWalletService;
  private CliConfig cliConfig;

  public CliStatusOrchestrator(
      int loopDelay, CliWalletService cliWalletService, CliConfig cliConfig) {
    super(loopDelay);
    if (CliUtils.hasConsole()) {
      this.statusInteractiveOrchestrator =
          new CliStatusInteractiveOrchestrator(loopDelay, cliWalletService, cliConfig);
    }
    this.cliWalletService = cliWalletService;
    this.cliConfig = cliConfig;
  }

  @Override
  public synchronized void start(boolean daemon) {
    super.start(daemon);
    if (statusInteractiveOrchestrator != null) {
      statusInteractiveOrchestrator.start(true);
    }
  }

  @Override
  public synchronized void stop() {
    super.stop();
    if (statusInteractiveOrchestrator != null) {
      statusInteractiveOrchestrator.stop();
    }
  }

  @Override
  protected void runOrchestrator() {
    printState();
  }

  private void printState() {
    try {
      WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();
      MixingState mixingState = whirlpoolWallet.getMixingState();
      WhirlpoolWalletConfig walletConfig = whirlpoolWallet.getConfig();

      System.out.print(
          "⣿ Wallet OPENED, mix "
              + (mixingState.isStarted() ? "STARTED" : "STOPPED")
              + (walletConfig.isAutoTx0() ? " +autoTx0=" + walletConfig.getAutoTx0PoolId() : "")
              + (walletConfig.isAutoMix() ? " +autoMix" : "")
              + (cliConfig.getTor() ? " +Tor" : "")
              + (cliConfig.isDojoEnabled() ? " +Dojo" : "")
              + ", "
              + mixingState.getNbMixing()
              + " mixing ("
              + mixingState.getNbMixingMustMix()
              + "+"
              + mixingState.getNbMixingLiquidity()
              + "), "
              + mixingState.getNbQueued()
              + " queued ("
              + mixingState.getNbQueuedMustMix()
              + "+"
              + mixingState.getNbQueuedLiquidity()
              + "). Commands: [T]hreads, [D]eposit, [P]remix, P[O]stmix, [S]ystem\r");
    } catch (NoSessionWalletException e) {
      System.out.print("⣿ Wallet CLOSED\r");
    } catch (Exception e) {
      log.error("", e);
    }
  }
}
