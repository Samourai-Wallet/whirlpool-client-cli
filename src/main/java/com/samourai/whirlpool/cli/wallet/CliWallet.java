package com.samourai.whirlpool.cli.wallet;

import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.services.WalletAggregateService;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.exception.EmptyWalletException;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliWallet extends WhirlpoolWallet {
  private final Logger log = LoggerFactory.getLogger(CliWallet.class);
  private CliConfig cliConfig;
  private WalletAggregateService walletAggregateService;

  public CliWallet(WhirlpoolWallet whirlpoolWallet, CliConfig cliConfig, WalletAggregateService walletAggregateService) {
    super(whirlpoolWallet);
    this.cliConfig = cliConfig;
    this.walletAggregateService = walletAggregateService;
  }

  @Override
  public void onEmptyWalletException(EmptyWalletException e) {
    try {
      autoRefill(e);
    } catch(Exception ee) {
      log.error("", ee);

      // default log
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

    long missingBalance = totalBalance - requiredBalance;
    if (log.isDebugEnabled()) {
      log.debug("requiredBalance=" + requiredBalance + " => missingBalance=" + missingBalance);
    }
    if (totalBalance < requiredBalance) {
      throw new EmptyWalletException("Insufficient balance to continue", missingBalance);
    }

    String depositAddress = getDepositAddress(false);
    String message = e.getMessageDeposit(depositAddress);
    if (!cliConfig.getMix().isAutoAggregatePostmix()) {
      CliUtils.waitUserAction(message);
      return;
    }

    // auto aggregate postmix
    log.info(" • depositWallet wallet is empty. Aggregating postmix to refill it...");
    boolean aggregateSuccess = walletAggregateService.consolidateTestnet();
    if (!aggregateSuccess) {
      CliUtils.waitUserAction(message);
    }
  }

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
