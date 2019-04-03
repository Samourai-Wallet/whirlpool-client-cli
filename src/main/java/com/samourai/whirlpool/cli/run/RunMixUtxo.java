package com.samourai.whirlpool.cli.run;

import com.samourai.whirlpool.cli.CliListener;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPremixHandler;
import com.samourai.whirlpool.client.mix.handler.PremixHandler;
import com.samourai.whirlpool.client.mix.handler.UtxoWithBalance;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunMixUtxo {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private WhirlpoolClientConfig whirlpoolClientConfig;
  private CliWalletService cliWalletService;
  private NetworkParameters params;

  public RunMixUtxo(
      WhirlpoolClientConfig whirlpoolClientConfig,
      CliWalletService cliWalletService,
      NetworkParameters params) {
    this.whirlpoolClientConfig = whirlpoolClientConfig;
    this.cliWalletService = cliWalletService;
    this.params = params;
  }

  public void run(String utxoHash, long utxoIdx, String utxoKey, long utxoBalance)
      throws Exception {

    // pools
    Collection<Pool> poolsByPreference =
        cliWalletService.getSessionWallet().findPoolsByPreferenceForPremix(utxoBalance, false);
    if (poolsByPreference.isEmpty()) {
      throw new NotifiableException("No pool for this utxo balance: " + utxoBalance);
    }
    Pool pool = poolsByPreference.iterator().next();

    // utxo key
    DumpedPrivateKey dumpedPrivateKey = new DumpedPrivateKey(params, utxoKey);
    ECKey ecKey = dumpedPrivateKey.getKey();

    // whirlpool
    UtxoWithBalance utxo = new UtxoWithBalance(utxoHash, utxoIdx, utxoBalance);
    IPremixHandler premixHandler = new PremixHandler(utxo, ecKey);
    IPostmixHandler postmixHandler = cliWalletService.getSessionWallet().computePostmixHandler();
    MixParams mixParams =
        new MixParams(pool.getPoolId(), pool.getDenomination(), premixHandler, postmixHandler);
    CliListener listener = new CliListener();
    whirlpoolClientConfig.newClient().whirlpool(mixParams, listener);
    listener.waitDone();
  }
}
