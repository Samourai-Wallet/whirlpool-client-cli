package com.samourai.whirlpool.client.wallet;

import com.samourai.api.client.SamouraiApi;
import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.wallet.client.indexHandler.FileIndexHandler;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactoryGeneric;
import com.samourai.wallet.hd.java.HD_WalletFactoryJava;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.wallet.pushTx.PushTxService;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliWallet extends WhirlpoolWallet {
  private final Logger log = LoggerFactory.getLogger(CliWallet.class);
  private static final HD_WalletFactoryGeneric hdWalletFactory = HD_WalletFactoryJava.getInstance();

  private static final int ACCOUNT_DEPOSIT = 0;
  private static final int ACCOUNT_PREMIX = Integer.MAX_VALUE - 2;
  private static final int ACCOUNT_POSTMIX = Integer.MAX_VALUE - 1;

  public static final String INDEX_BIP84_INITIALIZED = "bip84init";
  private static final String INDEX_DEPOSIT = "deposit";
  private static final String INDEX_PREMIX = "premix";
  private static final String INDEX_POSTMIX = "postmix";
  private static final String INDEX_FEE = "fee";

  private static final String FEE_XPUB =
      "vpub5YS8pQgZKVbrSn9wtrmydDWmWMjHrxL2mBCZ81BDp7Z2QyCgTLZCrnBprufuoUJaQu1ZeiRvUkvdQTNqV6hS96WbbVZgweFxYR1RXYkBcKt";
  private static final long FEE_VALUE = 10000; // TODO

  public static CliWallet get(
      NetworkParameters params,
      SamouraiApi samouraiApi,
      PushTxService pushTxService,
      WhirlpoolClient whirlpoolClient,
      String seedWords,
      String seedPassphrase,
      FileIndexHandler fileIndexHandler)
      throws Exception {
    // init wallet from seed
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, seedPassphrase, params);

    // init bip84 at first run
    boolean initBip84 = (fileIndexHandler.get(INDEX_BIP84_INITIALIZED) != 1);

    // deposit, premix & postmix wallets
    IIndexHandler depositIndexHandler = fileIndexHandler.getIndexHandler(INDEX_DEPOSIT);
    IIndexHandler premixIndexHandler = fileIndexHandler.getIndexHandler(INDEX_PREMIX);
    IIndexHandler postmixIndexHandler = fileIndexHandler.getIndexHandler(INDEX_POSTMIX);
    Bip84ApiWallet depositWallet =
        new Bip84ApiWallet(bip84w, ACCOUNT_DEPOSIT, depositIndexHandler, samouraiApi, initBip84);
    Bip84ApiWallet premixWallet =
        new Bip84ApiWallet(bip84w, ACCOUNT_PREMIX, premixIndexHandler, samouraiApi, initBip84);
    Bip84ApiWallet postmixWallet =
        new Bip84ApiWallet(bip84w, ACCOUNT_POSTMIX, postmixIndexHandler, samouraiApi, initBip84);

    // save initialized state
    if (initBip84) {
      fileIndexHandler.set(INDEX_BIP84_INITIALIZED, 1);
    }

    // services
    IIndexHandler feeIndexHandler = fileIndexHandler.getIndexHandler(INDEX_FEE);
    Tx0Service tx0Service = new Tx0Service(params, FEE_XPUB, FEE_VALUE);
    return new CliWallet(
        params,
        samouraiApi,
        pushTxService,
        tx0Service,
        whirlpoolClient,
        feeIndexHandler,
        depositWallet,
        premixWallet,
        postmixWallet);
  }

  private CliWallet(
      NetworkParameters params,
      SamouraiApi samouraiApi,
      PushTxService pushTxService,
      Tx0Service tx0Service,
      WhirlpoolClient whirlpoolClient,
      IIndexHandler feeIndexHandler,
      Bip84ApiWallet depositWallet,
      Bip84ApiWallet premixWallet,
      Bip84ApiWallet postmixWallet)
      throws Exception {
    super(
        params,
        samouraiApi,
        pushTxService,
        tx0Service,
        whirlpoolClient,
        feeIndexHandler,
        depositWallet,
        premixWallet,
        postmixWallet);
  }

  @Override
  public Bip84ApiWallet getDepositWallet() {
    return super.getDepositWallet();
  }

  @Override
  public Bip84ApiWallet getPremixWallet() {
    return super.getPremixWallet();
  }

  @Override
  public Bip84ApiWallet getPostmixWallet() {
    return super.getPostmixWallet();
  }
}
