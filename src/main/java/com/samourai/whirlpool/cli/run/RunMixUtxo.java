package com.samourai.whirlpool.cli.run;

import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.whirlpool.cli.CliListener;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.Bip84PostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPremixHandler;
import com.samourai.whirlpool.client.mix.handler.PremixHandler;
import com.samourai.whirlpool.client.mix.handler.UtxoWithBalance;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.lang.invoke.MethodHandles;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunMixUtxo {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private WhirlpoolClient whirlpoolClient;
  private CliWalletService cliWalletService;
  private NetworkParameters params;

  public RunMixUtxo(
      WhirlpoolClient whirlpoolClient,
      CliWalletService cliWalletService,
      NetworkParameters params) {
    this.whirlpoolClient = whirlpoolClient;
    this.cliWalletService = cliWalletService;
    this.params = params;
  }

  public void run(
      Pool pool, String utxoHash, long utxoIdx, String utxoKey, long utxoBalance, int mixs)
      throws Exception {

    // utxo key
    DumpedPrivateKey dumpedPrivateKey = new DumpedPrivateKey(params, utxoKey);
    ECKey ecKey = dumpedPrivateKey.getKey();
    Bip84ApiWallet postmixWallet = cliWalletService.getCliWallet().getPostmixWallet();

    // whirlpool
    UtxoWithBalance utxo = new UtxoWithBalance(utxoHash, utxoIdx, utxoBalance);
    IPremixHandler premixHandler = new PremixHandler(utxo, ecKey);
    IPostmixHandler postmixHandler = new Bip84PostmixHandler(postmixWallet);
    MixParams mixParams =
        new MixParams(pool.getPoolId(), pool.getDenomination(), premixHandler, postmixHandler);
    CliListener listener = new CliListener();
    whirlpoolClient.whirlpool(mixParams, mixs, listener);
    listener.waitDone();
  }
}
