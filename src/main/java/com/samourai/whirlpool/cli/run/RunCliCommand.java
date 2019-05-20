package com.samourai.whirlpool.cli.run;

import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.cli.ApplicationArgs;
import com.samourai.whirlpool.cli.services.CliConfigService;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.services.WalletAggregateService;
import com.samourai.whirlpool.cli.wallet.CliWallet;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import java.lang.invoke.MethodHandles;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunCliCommand {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private ApplicationArgs appArgs;
  private WhirlpoolClient whirlpoolClient;
  private WhirlpoolClientConfig whirlpoolClientConfig;
  private CliWalletService cliWalletService;
  private Bech32UtilGeneric bech32Util;
  private WalletAggregateService walletAggregateService;
  private CliConfigService cliConfigService;

  public RunCliCommand(
      ApplicationArgs appArgs,
      WhirlpoolClient whirlpoolClient,
      WhirlpoolClientConfig whirlpoolClientConfig,
      CliWalletService cliWalletService,
      Bech32UtilGeneric bech32Util,
      WalletAggregateService walletAggregateService,
      CliConfigService cliConfigService) {
    this.appArgs = appArgs;
    this.whirlpoolClient = whirlpoolClient;
    this.whirlpoolClientConfig = whirlpoolClientConfig;
    this.cliWalletService = cliWalletService;
    this.bech32Util = bech32Util;
    this.walletAggregateService = walletAggregateService;
    this.cliConfigService = cliConfigService;
  }

  public void run() throws Exception {
    NetworkParameters params = whirlpoolClientConfig.getNetworkParameters();

    if (appArgs.isDumpPayload()) {
      new RunDumpPayload(cliWalletService).run();
    } else if (ApplicationArgs.isMainAutoAggregatePostmix()) {
      CliWallet cliWallet = cliWalletService.getSessionWallet();

      // go aggregate and consolidate
      walletAggregateService.consolidateWallet(cliWallet);

      // should we move to a specific address?
      String toAddress = appArgs.getAggregatePostmix();
      if (toAddress != null && !"true".equals(toAddress)) {
        Bip84ApiWallet depositWallet = cliWallet.getWalletDeposit();
        log.info(" • Moving funds to: " + toAddress);
        walletAggregateService.toAddress(depositWallet, toAddress);
      }
    } else if (appArgs.isListPools()) {
      new RunListPools(whirlpoolClient).run();
    } else {
      throw new Exception("Unknown command.");
    }

    if (log.isDebugEnabled()) {
      log.debug("RunCliCommand success.");
    }
  }

  public static boolean hasCommandToRun(ApplicationArgs appArgs) {
    return appArgs.isDumpPayload() || appArgs.isMainAutoAggregatePostmix() || appArgs.isListPools();
  }
}
