package com.samourai.whirlpool.cli.run;

import com.samourai.api.client.SamouraiApi;
import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.services.WalletAggregateService;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.exception.EmptyWalletException;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.utils.ClientUtils;
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
  private static final int OUTPUTS_PER_TX0 = 5;

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

  public void run(Pool pool, boolean isAutoAggregatePostmix) throws Exception {
    while (true) {
      // wait for wallet startup... then wait for clientDelay before checking again...
      Thread.sleep((cliConfig.getMix().getClientDelay() * 1000) + SLEEP_LOOPWALLET);
      try {
        runLoopIteration(pool, isAutoAggregatePostmix);
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

  private void runLoopIteration(Pool pool, boolean isAutoAggregatePostmix) throws Exception {
    WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();
    WhirlpoolWalletState whirlpoolWalletState = whirlpoolWallet.getState();
    MixOrchestratorState mixState = whirlpoolWalletState.getMixState();
    log.info(
        "[Whirlpool state] "
            + mixState.getNbMixing()
            + "/"
            + mixState.getNbMax()
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

    int missingMustMixUtxos = whirlpoolWalletState.getMixState().getNbIdle();
    if (missingMustMixUtxos > 0) {
      if (log.isDebugEnabled()) {
        log.info("=> Idle threads detected, running " + missingMustMixUtxos + " Tx0s...");
      }
      // not enough mustMixUtxos => Tx0
      for (int i = 0; i < missingMustMixUtxos; i++) {
        log.info(" • Tx0 (" + (i + 1) + "/" + missingMustMixUtxos + ")...");
        doRunTx0(pool, missingMustMixUtxos, isAutoAggregatePostmix);
        samouraiApi.refreshUtxos();
      }
    }
  }

  private void doRunTx0(Pool pool, int missingMustMixUtxos, boolean isAutoAggregatePostmix)
      throws Exception {
    boolean success = false;
    while (!success) {
      try {
        WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();
        WhirlpoolUtxo spendFrom = whirlpoolWallet.findUtxoDepositForTx0(pool, OUTPUTS_PER_TX0, 1);
        whirlpoolWallet.tx0(pool, OUTPUTS_PER_TX0, spendFrom);
        success = true;
      } catch (EmptyWalletException e) {
        // deposit is empty => autoRefill when possible

        long requiredBalance =
            tx0Service.computeSpendFromBalanceMin(pool, samouraiApi.fetchFees(), OUTPUTS_PER_TX0)
                * missingMustMixUtxos;
        autoRefill(requiredBalance, isAutoAggregatePostmix);
      }
    }
  }

  private void autoRefill(long requiredBalance, boolean isAutoAggregatePostmix) throws Exception {
    Bip84ApiWallet depositWallet = cliWalletService.getSessionWallet().getWalletDeposit();
    Bip84ApiWallet premixWallet = cliWalletService.getSessionWallet().getWalletPremix();
    Bip84ApiWallet postmixWallet = cliWalletService.getSessionWallet().getWalletPostmix();

    // check total balance
    long depositBalance = depositWallet.fetchBalance();
    long premixBalance = premixWallet.fetchBalance();
    long postmixBalance = postmixWallet.fetchBalance();
    long totalBalance = depositBalance + premixBalance + postmixBalance;
    if (log.isDebugEnabled()) {
      log.debug("depositBalance=" + depositBalance);
      log.debug("premixBalance=" + premixBalance);
      log.debug("postmixBalance=" + postmixBalance);
      log.debug("totalBalance=" + totalBalance);
    }

    long missingBalance = totalBalance - requiredBalance;
    if (log.isDebugEnabled()) {
      log.debug("requiredBalance=" + requiredBalance + " => missingBalance=" + missingBalance);
    }
    if (totalBalance < requiredBalance) {
      throw new EmptyWalletException("Insufficient balance to continue", missingBalance);
    }

    String depositAddress =
        Bech32UtilGeneric.getInstance()
            .toBech32(depositWallet.getNextAddress(false), cliConfig.getNetworkParameters());
    String message =
        "Insufficient balance to continue. I need at least "
            + ClientUtils.satToBtc(missingBalance)
            + "btc to continue.\nPlease make a deposit to "
            + depositAddress;
    if (!isAutoAggregatePostmix) {
      CliUtils.waitUserAction(message);
      return;
    }

    // auto aggregate postmix
    log.info(" • depositWallet wallet is empty. Aggregating postmix to refill it...");
    boolean aggregateSuccess = walletAggregateService.consolidateTestnet();
    if (!aggregateSuccess) {
      CliUtils.waitUserAction(message);
    }

    samouraiApi.refreshUtxos();
  }
}
