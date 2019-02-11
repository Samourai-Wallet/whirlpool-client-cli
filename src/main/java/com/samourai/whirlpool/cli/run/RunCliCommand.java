package com.samourai.whirlpool.cli.run;

import com.samourai.api.client.SamouraiApi;
import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.cli.ApplicationArgs;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.services.CliTorClientService;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.services.WalletAggregateService;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Pools;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunCliCommand {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private ApplicationArgs appArgs;
  private CliConfig cliConfig;
  private SamouraiApi samouraiApi;
  private WhirlpoolClient whirlpoolClient;
  private WhirlpoolClientConfig whirlpoolClientConfig;
  private CliWalletService cliWalletService;
  private Bech32UtilGeneric bech32Util;
  private WalletAggregateService walletAggregateService;
  private CliTorClientService torClientService;
  private Tx0Service tx0Service;

  public RunCliCommand(
      ApplicationArgs appArgs,
      CliConfig cliConfig,
      SamouraiApi samouraiApi,
      WhirlpoolClient whirlpoolClient,
      WhirlpoolClientConfig whirlpoolClientConfig,
      CliWalletService cliWalletService,
      Bech32UtilGeneric bech32Util,
      WalletAggregateService walletAggregateService,
      CliTorClientService torClientService,
      Tx0Service tx0Service) {
    this.appArgs = appArgs;
    this.cliConfig = cliConfig;
    this.samouraiApi = samouraiApi;
    this.whirlpoolClient = whirlpoolClient;
    this.whirlpoolClientConfig = whirlpoolClientConfig;
    this.cliWalletService = cliWalletService;
    this.bech32Util = bech32Util;
    this.walletAggregateService = walletAggregateService;
    this.torClientService = torClientService;
    this.tx0Service = tx0Service;
  }

  public void run() throws Exception {
    NetworkParameters params = whirlpoolClientConfig.getNetworkParameters();
    Pools pools = whirlpoolClient.fetchPools();
    String poolId = appArgs.getPoolId();
    Pool pool = pools.findPoolById(poolId);

    if (appArgs.isUtxo()) {
      // go whirlpool with UTXO
      String utxoHash = appArgs.getUtxoHash();
      long utxoIdx = appArgs.getUtxoIdx();
      String utxoKey = appArgs.getUtxoKey();
      long utxoBalance = appArgs.getUtxoBalance();
      final int mixs = appArgs.getMixs();

      new RunMixUtxo(whirlpoolClientConfig, cliWalletService, params)
          .run(pool, utxoHash, utxoIdx, utxoKey, utxoBalance, mixs);
    } else {
      Optional<Integer> tx0Arg = appArgs.getTx0();
      if (tx0Arg.isPresent()) {
        // go tx0
        int nbOutputs = tx0Arg.get();
        WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();
        WhirlpoolUtxo spendFrom = whirlpoolWallet.findUtxoDepositForTx0(pool, nbOutputs, 1);
        whirlpoolWallet.tx0(pool, nbOutputs, spendFrom);
      } else if (appArgs.isAggregatePostmix()) {
        // go aggregate and consolidate
        walletAggregateService.consolidateTestnet();

        // should we move to a specific address?
        String toAddress = appArgs.getAggregatePostmix();
        if (toAddress != null) {
          Bip84ApiWallet depositWallet = cliWalletService.getSessionWallet().getWalletDeposit();
          if ("true".equals(toAddress)) {
            // aggregate to deposit
            toAddress = bech32Util.toBech32(depositWallet.getNextAddress(), params);
            log.info(" • Moving funds to deposit: " + toAddress);
          } else {
            log.info(" • Moving funds to: " + toAddress);
          }
          walletAggregateService.toAddress(depositWallet, toAddress);
        }
      } else {
        // go loop wallet
        RunLoopWallet runLoopWallet =
            new RunLoopWallet(
                cliConfig, samouraiApi, cliWalletService, walletAggregateService, tx0Service);
        runLoopWallet.run();
      }
    }
  }

  public static boolean hasCommandToRun(ApplicationArgs appArgs) {
    return appArgs.isUtxo() || appArgs.getTx0().isPresent() || appArgs.isAggregatePostmix();
  }
}
