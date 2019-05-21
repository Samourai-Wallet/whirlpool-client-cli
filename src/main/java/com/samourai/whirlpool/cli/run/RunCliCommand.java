package com.samourai.whirlpool.cli.run;

import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.whirlpool.cli.ApplicationArgs;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.services.WalletAggregateService;
import com.samourai.whirlpool.cli.wallet.CliWallet;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunCliCommand {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private ApplicationArgs appArgs;
  private CliWalletService cliWalletService;
  private WalletAggregateService walletAggregateService;
  private CliConfig cliConfig;

  public RunCliCommand(
      ApplicationArgs appArgs,
      CliWalletService cliWalletService,
      WalletAggregateService walletAggregateService,
      CliConfig cliConfig) {
    this.appArgs = appArgs;
    this.cliWalletService = cliWalletService;
    this.walletAggregateService = walletAggregateService;
    this.cliConfig = cliConfig;
  }

  public void run() throws Exception {
    if (appArgs.isDumpPayload()) {
      new RunDumpPayload(cliWalletService).run();
    } else if (appArgs.isAggregatePostmix()) {
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
      new RunListPools(cliWalletService, cliConfig).run();
    } else {
      throw new Exception("Unknown command.");
    }

    if (log.isDebugEnabled()) {
      log.debug("RunCliCommand success.");
    }
  }

  public static boolean hasCommandToRun(ApplicationArgs appArgs, CliConfig cliConfig) {
    return appArgs.isDumpPayload() || appArgs.isAggregatePostmix() || appArgs.isListPools();
  }
}
