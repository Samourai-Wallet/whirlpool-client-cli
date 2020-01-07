package com.samourai.whirlpool.cli.run;

import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.exception.NoSessionWalletException;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.MixingState;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.orchestrator.AbstractOrchestrator;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliStatusInteractiveOrchestrator extends AbstractOrchestrator {
  private static final Logger log = LoggerFactory.getLogger(CliStatusInteractiveOrchestrator.class);

  private CliWalletService cliWalletService;
  private CliConfig cliConfig;

  public CliStatusInteractiveOrchestrator(
      int loopDelay, CliWalletService cliWalletService, CliConfig cliConfig) {
    super(loopDelay);
    this.cliWalletService = cliWalletService;
    this.cliConfig = cliConfig;
  }

  @Override
  protected void runOrchestrator() {
    interactive();
  }

  private void interactive() {
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
          } else if (car.equals('S')) {
            printSystem();
          }
        } else {
          synchronized (this) {
            // when redirecting input
            wait(5000);
          }
        }
      } catch (Exception e) {
        log.error("", e);
      }
    }
  }

  private void printThreads() {
    try {
      WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();
      MixingState mixingState = whirlpoolWallet.getMixingState();
      log.info("⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿");
      log.info("⣿ MIXING THREADS:");
      int i = 0;
      for (WhirlpoolUtxo whirlpoolUtxo : mixingState.getUtxosMixing()) {
        log.info(
            "⣿ Thread #"
                + (i + 1)
                + ": MIXING "
                + whirlpoolUtxo.toString()
                + " ; "
                + whirlpoolUtxo.getUtxoConfig());
        i++;
      }
    } catch (NoSessionWalletException e) {
      System.out.print("⣿ Wallet CLOSED\r");
    } catch (Exception e) {
      log.error("", e);
    }
  }

  private void printSystem() {
    ThreadGroup tg = Thread.currentThread().getThreadGroup();
    Collection<Thread> threadSet =
        Thread.getAllStackTraces()
            .keySet()
            .stream()
            .filter(t -> t.getThreadGroup() == tg)
            .sorted(Comparator.comparing(o -> o.getName().toLowerCase()))
            .collect(Collectors.toList());
    log.info("⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿");
    log.info("⣿ SYSTEM THREADS:");
    int i = 0;
    for (Thread t : threadSet) {
      log.info("#" + i + " " + t + ":" + "" + t.getState());
      i++;
    }

    // memory
    Runtime rt = Runtime.getRuntime();
    long total = rt.totalMemory();
    long free = rt.freeMemory();
    long used = total - free;
    log.info("⣿ MEM USE: " + CliUtils.bytesToMB(used) + "M/" + CliUtils.bytesToMB(total) + "M");
  }

  private void printUtxos(String account, Collection<WhirlpoolUtxo> utxos) {
    try {
      log.info("⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿");
      log.info("⣿ " + account + " UTXOS:");
      ClientUtils.logWhirlpoolUtxos(utxos, cliConfig.getMix().getMixsTarget());

    } catch (Exception e) {
      log.error("", e);
    }
  }
}
