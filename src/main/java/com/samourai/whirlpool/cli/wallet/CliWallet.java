package com.samourai.whirlpool.cli.wallet;

import com.samourai.stomp.client.JavaStompClient;
import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.run.CliStatusOrchestrator;
import com.samourai.whirlpool.cli.services.CliConfigService;
import com.samourai.whirlpool.cli.services.CliTorClientService;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.services.WalletAggregateService;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.exception.EmptyWalletException;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.mix.listener.MixSuccess;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.whirlpool.listener.WhirlpoolClientListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliWallet extends WhirlpoolWallet {
  private static final Logger log = LoggerFactory.getLogger(CliWallet.class);
  private static final int CLI_STATUS_DELAY = 5000;

  private CliConfig cliConfig;
  private CliConfigService cliConfigService;
  private WalletAggregateService walletAggregateService;
  private CliStatusOrchestrator cliStatusOrchestrator;
  private JavaStompClient stompClient;
  private CliTorClientService cliTorClientService;

  public CliWallet(
      WhirlpoolWallet whirlpoolWallet,
      CliConfig cliConfig,
      CliConfigService cliConfigService,
      WalletAggregateService walletAggregateService,
      JavaStompClient stompClient,
      CliTorClientService cliTorClientService,
      CliWalletService cliWalletService) {
    super(whirlpoolWallet);
    this.cliConfig = cliConfig;
    this.cliConfigService = cliConfigService;
    this.walletAggregateService = walletAggregateService;
    this.stompClient = stompClient;
    this.cliTorClientService = cliTorClientService;

    // log status
    this.cliStatusOrchestrator =
        new CliStatusOrchestrator(CLI_STATUS_DELAY, cliWalletService, cliConfig);
  }

  @Override
  public void start() {
    if (!cliConfigService.isCliStatusReady()) {
      log.warn("Cannot start wallet: cliStatus is not ready");
      return;
    }
    // start wallet
    super.start();
    this.cliStatusOrchestrator.start();
  }

  @Override
  public void stop() {
    super.stop();
    this.cliStatusOrchestrator.stop();

    // disconnect STOMP
    stompClient.disconnect();
  }

  @Override
  public WhirlpoolClient mix(WhirlpoolUtxo whirlpoolUtxo, WhirlpoolClientListener notifyListener)
      throws NotifiableException {
    // get TOR ready before mixing
    cliTorClientService.waitReady();
    return super.mix(whirlpoolUtxo, notifyListener);
  }

  @Override
  public void onMixSuccess(MixSuccess mixSuccess, WhirlpoolUtxo whirlpoolUtxo) {
    super.onMixSuccess(mixSuccess, whirlpoolUtxo);

    // change TOR circuit
    cliTorClientService.changeCircuit();
  }

  @Override
  public synchronized void onEmptyWalletException(EmptyWalletException e) {
    try {
      if (cliConfig.isAutoAggregatePostmix()) {
        // run autoAggregatePostmix
        autoRefill(e);
      } else {
        // default management
        throw e;
      }
    } catch (Exception ee) {
      // default management
      super.onEmptyWalletException(e);
    }
  }

  private void autoRefill(EmptyWalletException e) throws Exception {
    long requiredBalance = e.getBalanceRequired();
    Bip84ApiWallet depositWallet = getWalletDeposit();
    Bip84ApiWallet premixWallet = getWalletPremix();
    Bip84ApiWallet postmixWallet = getWalletPostmix();

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

    long missingBalance = requiredBalance - totalBalance;
    if (log.isDebugEnabled()) {
      log.debug("requiredBalance=" + requiredBalance + " => missingBalance=" + missingBalance);
    }
    if (missingBalance > 0) {
      // cannot autoAggregatePostmix
      throw new EmptyWalletException("Insufficient balance to continue", missingBalance);
    }

    // auto aggregate postmix is possible
    log.info(" o AutoAggregatePostmix: depositWallet wallet is empty => aggregating");
    Exception aggregateException = null;
    try {
      boolean success = walletAggregateService.consolidateWallet(this);
      if (!success) {
        throw new NotifiableException("AutoAggregatePostmix failed (nothing to aggregate?)");
      }
      if (log.isDebugEnabled()) {
        log.debug("AutoAggregatePostmix SUCCESS. ");
      }
    } catch (Exception ee) {
      // resume wallet before throwing exception (to retry later)
      aggregateException = ee;
      if (log.isDebugEnabled()) {
        log.debug("AutoAggregatePostmix ERROR, will throw error later.");
      }
    }

    // reset mixing threads to avoid mixing obsolete consolidated utxos
    mixOrchestrator.stopMixingClients();

    clearCache();

    if (aggregateException != null) {
      throw aggregateException;
    }
  }

  @Override
  public void notifyError(String message) {
    CliUtils.notifyError(message);
  }

  public void interactive() {
    cliStatusOrchestrator.interactive();
  }

  // make public

  @Override
  public Bip84ApiWallet getWalletDeposit() {
    return super.getWalletDeposit();
  }

  @Override
  public Bip84ApiWallet getWalletPremix() {
    return super.getWalletPremix();
  }

  @Override
  public Bip84ApiWallet getWalletPostmix() {
    return super.getWalletPostmix();
  }
}
