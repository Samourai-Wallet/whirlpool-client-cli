package com.samourai.whirlpool.cli.run;

import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.cli.ApplicationArgs;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.services.WalletAggregateService;
import com.samourai.whirlpool.cli.wallet.CliWallet;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Pools;
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

  public RunCliCommand(
      ApplicationArgs appArgs,
      WhirlpoolClient whirlpoolClient,
      WhirlpoolClientConfig whirlpoolClientConfig,
      CliWalletService cliWalletService,
      Bech32UtilGeneric bech32Util,
      WalletAggregateService walletAggregateService) {
    this.appArgs = appArgs;
    this.whirlpoolClient = whirlpoolClient;
    this.whirlpoolClientConfig = whirlpoolClientConfig;
    this.cliWalletService = cliWalletService;
    this.bech32Util = bech32Util;
    this.walletAggregateService = walletAggregateService;
  }

  public void run() throws Exception {
    NetworkParameters params = whirlpoolClientConfig.getNetworkParameters();
    Pools pools = whirlpoolClient.fetchPools();

    if (appArgs.isUtxo()) {
      // go whirlpool with UTXO
      String utxoHash = appArgs.getUtxoHash();
      long utxoIdx = appArgs.getUtxoIdx();
      String utxoKey = appArgs.getUtxoKey();
      long utxoBalance = appArgs.getUtxoBalance();
      final int mixs = appArgs.getMixs();

      String poolId = appArgs.getPoolId();
      Pool pool = pools.findPoolById(poolId);

      new RunMixUtxo(whirlpoolClientConfig, cliWalletService, params)
          .run(pool, utxoHash, utxoIdx, utxoKey, utxoBalance, mixs);
    } else if (appArgs.isAggregatePostmix()) {
      CliWallet cliWallet = cliWalletService.getSessionWallet();

      // go aggregate and consolidate
      walletAggregateService.consolidateTestnet(cliWallet);

      // should we move to a specific address?
      String toAddress = appArgs.getAggregatePostmix();
      if (toAddress != null) {
        Bip84ApiWallet depositWallet = cliWallet.getWalletDeposit();
        if ("true".equals(toAddress)) {
          // aggregate to deposit
          toAddress = bech32Util.toBech32(depositWallet.getNextAddress(), params);
          log.info(" • Moving funds to deposit: " + toAddress);
        } else {
          log.info(" • Moving funds to: " + toAddress);
        }
        walletAggregateService.toAddress(depositWallet, toAddress);
      }
    } else if (appArgs.isListPools()) {
      new RunListPools(whirlpoolClient).run();
    } else {
      throw new Exception("Unknown command.");
    }
  }

  public static boolean hasCommandToRun(ApplicationArgs appArgs) {
    return appArgs.isUtxo() || appArgs.isAggregatePostmix() || appArgs.isListPools();
  }
}
