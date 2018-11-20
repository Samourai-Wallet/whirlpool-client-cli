package com.samourai.whirlpool.client.run;

import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.impl.Bip47Util;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryJava;
import com.samourai.whirlpool.client.CliListener;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPremixHandler;
import com.samourai.whirlpool.client.mix.handler.PostmixHandler;
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
  private static final HD_WalletFactoryJava hdWalletFactory = HD_WalletFactoryJava.getInstance();

  public RunMixUtxo() {}

  public void run(
      WhirlpoolClient whirlpoolClient,
      Pool pool,
      NetworkParameters params,
      String utxoHash,
      long utxoIdx,
      String utxoKey,
      long utxoBalance,
      HD_Wallet bip84w,
      int paynymIndex,
      int mixs)
      throws Exception {
    // utxo key
    DumpedPrivateKey dumpedPrivateKey = new DumpedPrivateKey(params, utxoKey);
    ECKey ecKey = dumpedPrivateKey.getKey();

    // init BIP47 wallet for input
    BIP47Wallet bip47w =
        hdWalletFactory.getBIP47(bip84w.getSeedHex(), bip84w.getPassphrase(), params);

    // whirlpool
    UtxoWithBalance utxo = new UtxoWithBalance(utxoHash, utxoIdx, utxoBalance);
    IPremixHandler premixHandler = new PremixHandler(utxo, ecKey);
    IPostmixHandler postmixHandler =
        new PostmixHandler(bip47w, paynymIndex, Bip47Util.getInstance());
    MixParams mixParams =
        new MixParams(pool.getPoolId(), pool.getDenomination(), premixHandler, postmixHandler);
    CliListener listener = new CliListener();
    whirlpoolClient.whirlpool(mixParams, mixs, listener);
    listener.waitDone();
  }
}
