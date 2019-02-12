package com.samourai.whirlpool.cli.run;

import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.exception.NoSessionWalletException;
import com.samourai.whirlpool.cli.wallet.CliWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.MixOrchestratorState;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolWalletState;
import com.samourai.whirlpool.client.wallet.orchestrator.AbstractOrchestrator;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliStatusOrchestrator extends AbstractOrchestrator {
  private static final Logger log = LoggerFactory.getLogger(CliStatusOrchestrator.class);

  private CliWallet whirlpoolWallet;
  private CliConfig cliConfig;

  public CliStatusOrchestrator(int loopDelay, CliWallet cliWallet, CliConfig cliConfig) {
    super(loopDelay);
    this.whirlpoolWallet = whirlpoolWallet;
    this.cliConfig = cliConfig;
  }

  @Override
  protected void runOrchestrator() {
    // log CLI status
    try {
      String poolsByPriorityStr = getPoolsByPriorityStr(whirlpoolWallet);

      log.info("---------------------------------------------------------------------");
      log.info(
          "[CLI] SESSION WALLET is OPENED and "
              + (whirlpoolWallet.isStarted() ? "STARTED" : "STOPPED")
              + ", autoTx0="
              + cliConfig.getMix().isAutoTx0()
              + ", autoMix="
              + cliConfig.getMix().isAutoMix()
              + ", autoAggregatePostmix="
              + cliConfig.getMix().isAutoAggregatePostmix()
              + ", poolsByPriority="
              + poolsByPriorityStr);
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
    } catch (Exception e) {
      log.error("", e);
    }
  }

  private String getPoolsByPriorityStr(WhirlpoolWallet whirlpoolWallet) throws Exception {
    Collection<Pool> poolsByPriority = whirlpoolWallet.getPoolsByPriority();
    List<String> poolIdsByPriority = new LinkedList<>();
    for (Pool pool : poolsByPriority) {
      poolIdsByPriority.add(pool.getPoolId());
    }
    if (poolIdsByPriority.isEmpty()) {
      return "null";
    }
    return Strings.join(poolIdsByPriority, ',');
  }
}
