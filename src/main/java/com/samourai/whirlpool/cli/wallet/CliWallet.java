package com.samourai.whirlpool.cli.wallet;

import com.samourai.http.client.CliHttpClient;
import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.services.CliConfigService;
import com.samourai.whirlpool.cli.services.CliTorClientService;
import com.samourai.whirlpool.cli.services.WalletAggregateService;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.exception.EmptyWalletException;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.mix.listener.MixSuccess;
import com.samourai.whirlpool.client.tx0.Tx0Config;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.MixProgress;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliWallet extends WhirlpoolWallet {
  private static final Logger log = LoggerFactory.getLogger(CliWallet.class);

  private CliConfig cliConfig;
  private CliConfigService cliConfigService;
  private WalletAggregateService walletAggregateService;
  private CliTorClientService cliTorClientService;
  private CliHttpClient httpClient;

  public CliWallet(
      WhirlpoolWallet whirlpoolWallet,
      CliConfig cliConfig,
      CliConfigService cliConfigService,
      WalletAggregateService walletAggregateService,
      CliTorClientService cliTorClientService,
      CliHttpClient httpClient) {
    super(whirlpoolWallet);
    this.cliConfig = cliConfig;
    this.cliConfigService = cliConfigService;
    this.walletAggregateService = walletAggregateService;
    this.cliTorClientService = cliTorClientService;
    this.httpClient = httpClient;
  }

  @Override
  public void start() {
    if (!cliConfigService.isCliStatusReady()) {
      log.warn("Cannot start wallet: cliStatus is not ready");
      return;
    }
    // start wallet
    super.start();
  }

  @Override
  public void stop() {
    super.stop();
  }

  @Override
  public Observable<MixProgress> mix(WhirlpoolUtxo whirlpoolUtxo) throws NotifiableException {
    // get Tor ready before mixing
    cliTorClientService.waitReady();
    return super.mix(whirlpoolUtxo);
  }

  @Override
  public void onMixSuccess(WhirlpoolUtxo whirlpoolUtxo, MixSuccess mixSuccess) {
    super.onMixSuccess(whirlpoolUtxo, mixSuccess);

    // change Tor identity
    cliTorClientService.changeIdentity();
    httpClient.changeIdentity();
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
  public Tx0Config getTx0Config(Pool pool) {
    Tx0Config tx0Config = super.getTx0Config(pool);

    // maxOutputs
    if (cliConfig.getMix().getTx0MaxOutputs() > 0) {
      int maxOutputs = cliConfig.getMix().getTx0MaxOutputs();
      tx0Config.setMaxOutputs(maxOutputs);
    }

    // overspend
    String poolId = pool.getPoolId();
    Long overspendOrNull =
        cliConfig.getMix().getOverspend() != null
            ? cliConfig.getMix().getOverspend().get(poolId)
            : null;
    if (overspendOrNull != null) {
      tx0Config.setOverspend(overspendOrNull);
    }
    return tx0Config;
  }

  @Override
  public void notifyError(String message) {
    CliUtils.notifyError(message);
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
