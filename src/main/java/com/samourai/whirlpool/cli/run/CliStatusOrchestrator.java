package com.samourai.whirlpool.cli.run;

import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.exception.NoSessionWalletException;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletConfig;
import com.samourai.whirlpool.client.wallet.beans.MixOrchestratorState;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolWalletState;
import com.samourai.whirlpool.client.wallet.orchestrator.AbstractOrchestrator;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliStatusOrchestrator extends AbstractOrchestrator {
  private static final Logger log = LoggerFactory.getLogger(CliStatusOrchestrator.class);

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
    printState();
  }

  public void interactive() {
    while (true) {
      try {
        Character car = CliUtils.readChar();
        if (car != null) {
          if (car.equals('T')) {
            printThreads();
          } else if (car.equals('D')) {
            WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();
            printUtxos("DEPOSIT", whirlpoolWallet.getUtxosDeposit());
          } else if (car.equals('P')) {
            WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();
            printUtxos("PREMIX", whirlpoolWallet.getUtxosPremix());
          } else if (car.equals('O')) {
            WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();
            printUtxos("POSTMIX", whirlpoolWallet.getUtxosPostmix());
          }
        } else {
          synchronized (this) {
            // when redirecting input
            wait(5000);
          }
        }
        printState();
      } catch (Exception e) {
        log.error("", e);
      }
    }
  }

  private void printState() {
    try {
      WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();
      WhirlpoolWalletState whirlpoolWalletState = whirlpoolWallet.getState();
      MixOrchestratorState mixState = whirlpoolWalletState.getMixState();

      WhirlpoolWalletConfig walletConfig = whirlpoolWallet.getConfig();

      System.out.print(
          "⣿ Wallet OPENED, mix "
              + (whirlpoolWallet.isStarted() ? "STARTED" : "STOPPED")
              + (walletConfig.isAutoTx0() ? " +autoTx0=" + walletConfig.getAutoTx0PoolId() : "")
              + (walletConfig.isAutoMix() ? " +autoMix" : "")
              + (cliConfig.getTor() ? " +Tor" : "")
              + (cliConfig.isDojoEnabled() ? " +Dojo" : "")
              + ", "
              + mixState.getNbMixing()
              + " mixing, "
              + mixState.getNbIdle()
              + " idle, "
              + mixState.getNbQueued()
              + " queued. Commands: [T]hreads, [D]eposit, [P]remix, P[O]stmix\r");
    } catch (NoSessionWalletException e) {
      System.out.print("⣿ Wallet CLOSED");
    } catch (Exception e) {
      log.error("", e);
    }
  }

  private void printThreads() {
    try {
      WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();
      WhirlpoolWalletState whirlpoolWalletState = whirlpoolWallet.getState();
      MixOrchestratorState mixState = whirlpoolWalletState.getMixState();
      log.info("⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿");
      log.info("⣿ THREADS:");
      int i = 0;
      for (WhirlpoolUtxo whirlpoolUtxo : mixState.getUtxosMixing()) {
        log.info(
            "⣿ Thread #"
                + (i + 1)
                + ": MIXING "
                + whirlpoolUtxo.toString()
                + " ; "
                + whirlpoolUtxo.getUtxoConfig());
        i++;
      }
      for (; i < cliConfig.getMix().getClients(); i++) {
        log.info("⣿ Thread #" + (i + 1) + ": idle");
      }
    } catch (NoSessionWalletException e) {
      System.out.print("⣿ Wallet CLOSED");
    } catch (Exception e) {
      log.error("", e);
    }
  }

  private void printUtxos(String account, Collection<WhirlpoolUtxo> utxos) {
    try {
      log.info("⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿");
      log.info("⣿ " + account + " UTXOS:");
      ClientUtils.logWhirlpoolUtxos(utxos);

    } catch (Exception e) {
      log.error("", e);
    }
  }
}
